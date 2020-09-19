// SPDX-License-Identifier: BSD-3-Clause
package org.islandoftex.arara.cli.interpreter

import java.nio.file.Files
import java.nio.file.Path
import org.islandoftex.arara.api.AraraException
import org.islandoftex.arara.api.configuration.ExecutionMode
import org.islandoftex.arara.api.rules.Directive
import org.islandoftex.arara.api.rules.DirectiveConditional
import org.islandoftex.arara.api.rules.Rule
import org.islandoftex.arara.api.session.Command
import org.islandoftex.arara.api.session.ExecutionStatus
import org.islandoftex.arara.cli.ruleset.RuleFormat
import org.islandoftex.arara.cli.ruleset.RuleUtils
import org.islandoftex.arara.cli.utils.DisplayUtils
import org.islandoftex.arara.core.files.FileHandling
import org.islandoftex.arara.core.localization.LanguageController
import org.islandoftex.arara.core.session.LinearExecutor
import org.islandoftex.arara.core.session.Session
import org.islandoftex.arara.core.ui.InputHandling
import org.islandoftex.arara.mvel.interpreter.AraraExceptionWithHeader
import org.islandoftex.arara.mvel.interpreter.HaltExpectedException
import org.islandoftex.arara.mvel.rules.DirectiveConditionalEvaluator
import org.islandoftex.arara.mvel.rules.RuleArgument
import org.islandoftex.arara.mvel.rules.SerialRuleCommand
import org.islandoftex.arara.mvel.utils.MvelState
import org.mvel2.templates.TemplateRuntime
import org.slf4j.LoggerFactory

/**
 * Interprets the list of directives.
 *
 * @author Island of TeX
 * @version 5.0
 * @since 4.0
 */
object Interpreter {
    // the class logger obtained from
    // the logger factory
    private val logger = LoggerFactory.getLogger(Interpreter::class.java)

    /**
     * Gets the rule according to the provided directive.
     *
     * @param directive The provided directive.
     * @return The absolute canonical path of the rule, given the provided
     * directive.
     * @throws AraraException Something wrong happened, to be caught in the
     * higher levels.
     */
    @Throws(AraraException::class)
    private fun getRule(directive: Directive): Path =
            LinearExecutor.executionOptions.rulePaths.let { paths ->
                paths.flatMap { path ->
                    listOf(
                            InterpreterUtils.construct(path, directive.identifier, RuleFormat.MVEL),
                            InterpreterUtils.construct(path, directive.identifier, RuleFormat.KOTLIN_DSL),
                            // this lookup adds support for the rules distributed with
                            // arara (in TL names should be unique, hence we avoided
                            // going for pdflatex.yaml in favor of arara-rule-pdflatex.yaml
                            // from version 6 on)
                            InterpreterUtils.construct(path, "arara-rule-" + directive.identifier, RuleFormat.MVEL)
                    )
                }.firstOrNull { Files.exists(it) } ?: throw AraraException(
                        LanguageController.messages.ERROR_INTERPRETER_RULE_NOT_FOUND.format(
                                directive.identifier,
                                directive.identifier,
                                paths.joinToString("; ", "(", ")") {
                                    FileHandling.normalize(it).toString()
                                }
                        )
                )
            }

    // TODO: in the following, extract the printing into the higher level
    // function
    /**
     * "Run" a boolean return value
     * @param value The boolean.
     * @param conditional The conditional to print in dry-run mode.
     * @param authors The authors of the rule.
     * @return Returns [value]
     */
    private fun runBoolean(
        value: Boolean,
        conditional: DirectiveConditional,
        authors: List<String>
    ): Boolean {
        logger.info(LanguageController.messages.LOG_INFO_BOOLEAN_MODE.format(value))

        if (LinearExecutor.executionOptions.executionMode == ExecutionMode.DRY_RUN) {
            DisplayUtils.printAuthors(authors)
            DisplayUtils.printWrapped(LanguageController.messages
                    .INFO_INTERPRETER_DRYRUN_MODE_BOOLEAN_MODE.format(value))
            DisplayUtils.printConditional(conditional)
        }

        return value
    }

    /**
     * Run a command
     *
     * @param command The command to run.
     * @param conditional The conditional applied to the run (only for printing).
     * @param authors The rule authors (only for printing).
     * @param ruleCommandExitValue The exit value of the rule command.
     * @return Success of the execution.
     * @throws AraraException Execution failed.
     */
    @Throws(AraraException::class)
    @Suppress("TooGenericExceptionCaught")
    private fun runCommand(
        command: Command,
        conditional: DirectiveConditional,
        authors: List<String>,
        ruleCommandExitValue: String?
    ): Boolean {
        logger.info(LanguageController.messages.LOG_INFO_SYSTEM_COMMAND.format(command))
        var success = true

        if (LinearExecutor.executionOptions.executionMode != ExecutionMode.DRY_RUN) {
            val code = InterpreterUtils.run(command)
            val check: Any = try {
                val context = mapOf<String, Any>("value" to code)
                TemplateRuntime.eval(
                        "@{ " + (ruleCommandExitValue ?: "value == 0") + " }",
                        context)
            } catch (exception: RuntimeException) {
                throw AraraExceptionWithHeader(LanguageController.messages
                        .ERROR_INTERPRETER_EXIT_RUNTIME_ERROR,
                        exception
                )
            }

            success = if (check is Boolean) {
                check
            } else {
                throw AraraExceptionWithHeader(
                        LanguageController.messages
                                .ERROR_INTERPRETER_WRONG_EXIT_CLOSURE_RETURN
                )
            }
        } else {
            DisplayUtils.printAuthors(authors)
            DisplayUtils.printWrapped(LanguageController.messages
                    .INFO_INTERPRETER_DRYRUN_MODE_SYSTEM_COMMAND.format(command))
            DisplayUtils.printConditional(conditional)
        }

        return success
    }

    /**
     * Converts the command evaluation result to a flat list.
     * @param result The result
     * @return A flat list.
     */
    private fun resultToList(result: Any) = if (result is List<*>) {
        InputHandling.flatten(result)
    } else {
        listOf(result)
    }

    /**
     * Execute a command.
     * @param command The command to evaluate.
     * @param conditional Under which condition to execute.
     * @param rule The rule (only passed for output purposes).
     * @param parameters The parameters for evaluation
     * @throws AraraException Running the command failed.
     */
    @Throws(AraraException::class)
    @Suppress("TooGenericExceptionCaught", "ThrowsCount")
    private fun executeCommand(
        command: SerialRuleCommand,
        conditional: DirectiveConditional,
        rule: Rule,
        parameters: Map<String, Any>
    ) = try {
        resultToList(TemplateRuntime.eval(command.commandString!!, parameters))
    } catch (exception: RuntimeException) {
        throw AraraExceptionWithHeader(LanguageController
                .messages.ERROR_INTERPRETER_COMMAND_RUNTIME_ERROR,
                exception
        )
    }.filter { it.toString().isNotBlank() }.forEach { current ->
        DisplayUtils.printEntry(rule.displayName!!, command.name
                ?: LanguageController.messages.INFO_LABEL_UNNAMED_TASK)

        val success = when (current) {
            is Boolean -> runBoolean(current, conditional,
                    rule.authors)
            is Command -> runCommand(current, conditional,
                    rule.authors, command.exit)
            else ->
                throw AraraExceptionWithHeader(LanguageController
                        .messages.ERROR_INTERPRETER_WRONG_RETURN_TYPE)
        }

        DisplayUtils.printEntryResult(success)

        if (LinearExecutor.executionOptions.haltOnErrors && !success)
            throw HaltExpectedException(LanguageController
                    .messages.ERROR_INTERPRETER_COMMAND_UNSUCCESSFUL_EXIT
                    .format(command.name))

        // TODO: document this key
        val haltKey = "arara:${LinearExecutor.currentFile!!.path.fileName}:halt"
        if (Session.contains(haltKey)) {
            LinearExecutor.executionStatus =
                    if (Session[haltKey].toString().toInt() != 0)
                        ExecutionStatus.EXTERNAL_CALL_FAILED
                    else
                        ExecutionStatus.PROCESSING
            throw HaltExpectedException(LanguageController.messages
                    .ERROR_INTERPRETER_USER_REQUESTED_HALT)
        }
    }

    /**
     * Executes each directive, throwing an exception if something bad has
     * happened.
     *
     * @throws AraraException Something wrong happened, to be caught in the
     * higher levels.
     */
    @Throws(AraraException::class)
    @Suppress("NestedBlockDepth")
    fun execute(directive: Directive): Int {
        logger.info(
                LanguageController.messages.LOG_INFO_INTERPRET_RULE.format(
                        directive.identifier
                )
        )

        val file = getRule(directive)
        logger.info(
                LanguageController.messages.LOG_INFO_RULE_LOCATION.format(
                        file.parent
                )
        )

        // parse the rule identified by the directive (may throw an exception)
        val rule = RuleUtils.parseRule(file, directive.identifier)
        val parameters = parseArguments(rule, directive).plus(MvelState.ruleMethods)
        val evaluator = DirectiveConditionalEvaluator(LinearExecutor.executionOptions)

        var available = true
        if (InterpreterUtils.runPriorEvaluation(directive.conditional)) {
            available = evaluator.evaluate(directive.conditional)
        }

        // if this directive is conditionally disabled, skip
        if (!available || Session.contains("arara:${LinearExecutor.currentFile!!.path.fileName}:halt"))
            return LinearExecutor.executionStatus.exitCode

        try {
            // if not execute the commands associated with the directive
            do {
                rule.commands.forEach { command ->
                    executeCommand(
                            // TODO: remove cast
                            command as SerialRuleCommand,
                            directive.conditional,
                            rule,
                            parameters
                    )
                }
            } while (evaluator.evaluate(directive.conditional))
        } catch (_: HaltExpectedException) {
            // If the user uses the halt rule to trigger a halt, this will be
            // raised. Any other exception will not be caught and propagate up.
        } catch (e: AraraExceptionWithHeader) {
            // rethrow arara exceptions that are bound to have a header by
            // prepending the header and removing the outer exception with
            // header where possible
            throw AraraException(
                    LanguageController.messages.ERROR_RULE_IDENTIFIER_AND_PATH
                            .format(directive.identifier, file.parent.toString()) + " " +
                            e.message, e.exception ?: e)
        }
        return LinearExecutor.executionStatus.exitCode
    }

    /**
     * Gets a set of strings containing unknown keys from a map and a list. It
     * is a set difference from the keys in the map and the entries in the list.
     *
     * @param parameters The map of parameters.
     * @param arguments The list of arguments.
     * @return A set of strings representing unknown keys from a map and a list.
     */
    private fun getUnknownKeys(
        parameters: Map<String, Any>,
        arguments: List<org.islandoftex.arara.api.rules.RuleArgument<*>>
    ): Set<String> {
        val found = parameters.keys
        val expected = arguments.map { it.identifier }
        return found.subtract(expected)
    }

    /**
     * Parses the rule arguments against the provided directive.
     *
     * @param rule The rule object.
     * @param directive The directive.
     * @return A map containing all arguments resolved according to the
     * directive parameters.
     * @throws AraraException Something wrong happened, to be caught in the
     * higher levels.
     */
    @Throws(AraraException::class)
    private fun parseArguments(rule: Rule, directive: Directive):
            Map<String, Any> {
        val unknown = getUnknownKeys(directive.parameters, rule.arguments)
                .minus("reference")
        if (unknown.isNotEmpty())
            throw AraraExceptionWithHeader(LanguageController.messages
                    .ERROR_INTERPRETER_UNKNOWN_KEYS.format(
                            unknown.joinToString(", ", "(", ")")
                    )
            )

        val resolvedArguments = mutableMapOf<String, Any>()
        resolvedArguments["reference"] = directive.parameters.getValue("reference")

        val context = mapOf(
                "parameters" to directive.parameters,
                "reference" to directive.parameters.getValue("reference")
        ).plus(MvelState.ruleMethods)

        rule.arguments.forEach { argument ->
            resolvedArguments[argument.identifier] = processArgument(
                    // TODO: remove cast
                    argument as RuleArgument,
                    directive.parameters.containsKey(argument.identifier),
                    context
            )
        }

        return resolvedArguments
    }

    /**
     * Process a single argument and return the evaluated result.
     *
     * @param argument The argument to process.
     * @param idInDirectiveParams Whether the argument's identifier is
     *   contained in the directive's parameters field.
     * @param context The context for the evaluation.
     * @return The result of the evaluation.
     * @throws AraraException The argument could not be processed.
     */
    @Throws(AraraException::class)
    @Suppress("TooGenericExceptionCaught", "ThrowsCount")
    private fun processArgument(
        argument: RuleArgument,
        idInDirectiveParams: Boolean,
        context: Map<String, Any>
    ): Any {
        if (argument.isRequired && !idInDirectiveParams)
            throw AraraExceptionWithHeader(
                    LanguageController.messages.ERROR_INTERPRETER_ARGUMENT_IS_REQUIRED
                            .format(argument.identifier)
            )

        return argument.flag?.takeIf { idInDirectiveParams }?.let {
            try {
                TemplateRuntime.eval(argument.flag!!, context)
            } catch (exception: RuntimeException) {
                throw AraraExceptionWithHeader(LanguageController.messages
                        .ERROR_INTERPRETER_FLAG_RUNTIME_EXCEPTION,
                        exception
                )
            }
        } ?: argument.defaultValue?.let {
            try {
                TemplateRuntime.eval(it, context)
            } catch (exception: RuntimeException) {
                throw AraraExceptionWithHeader(LanguageController.messages
                        .ERROR_INTERPRETER_DEFAULT_VALUE_RUNTIME_ERROR,
                        exception
                )
            }
        } ?: ""
    }
}
