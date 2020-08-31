// SPDX-License-Identifier: BSD-3-Clause
package org.islandoftex.arara.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.clikt.parameters.types.restrictTo
import java.util.Locale
import kotlin.time.TimeSource
import kotlin.time.milliseconds
import org.islandoftex.arara.api.AraraException
import org.islandoftex.arara.api.configuration.ExecutionMode
import org.islandoftex.arara.api.session.ExecutionStatus
import org.islandoftex.arara.cli.configuration.ConfigurationUtils
import org.islandoftex.arara.cli.ruleset.DirectiveUtils
import org.islandoftex.arara.cli.utils.DisplayUtils
import org.islandoftex.arara.cli.utils.LoggingUtils
import org.islandoftex.arara.core.configuration.ExecutionOptions
import org.islandoftex.arara.core.configuration.LoggingOptions
import org.islandoftex.arara.core.configuration.UserInterfaceOptions
import org.islandoftex.arara.core.files.FileHandling
import org.islandoftex.arara.core.files.FileSearching
import org.islandoftex.arara.core.files.Project
import org.islandoftex.arara.core.files.ProjectFile
import org.islandoftex.arara.core.localization.LanguageController
import org.islandoftex.arara.core.session.Executor
import org.islandoftex.arara.core.session.ExecutorHooks
import org.islandoftex.arara.core.session.Session

/**
 * arara's command line interface
 *
 * @author Island of TeX
 * @version 5.0
 * @since 5.0
 */
class CLI : CliktCommand(name = "arara", printHelpOnEmptyArgs = true) {
    private val log by option("-l", "--log",
            help = "Generate a log output")
            .flag()
    private val verbose by option("-v", "--verbose",
            help = "Print the command output")
            .flag("-s", "--silent")
    private val dryRun by option("-n", "--dry-run",
            help = "Go through all the motions of running a command, but " +
                    "with no actual calls")
            .flag()
    private val onlyHeader by option("-H", "--header",
            help = "Extract directives only in the file header")
            .flag()
    private val timeout by option("-t", "--timeout",
            help = "Set the execution timeout (in milliseconds)")
            .int().restrictTo(min = 1)
    private val language by option("-L", "--language",
            help = "Set the application language")
    private val maxLoops by option("-m", "--max-loops",
            help = "Set the maximum number of loops (> 0)")
            .int().restrictTo(min = 1)
    private val workingDirectory by option("-d", "--working-directory",
            help = "Set the working directory for all tools")
            .path(mustExist = true, canBeFile = false, mustBeReadable = true)
    private val parameters: Map<String, String> by option("-P", "--call-property",
            help = "Pass parameters to the application to be used within the " +
                    "session.")
            .associate()

    private val reference by argument("file",
            help = "The file(s) to evaluate and process")
            .multiple(required = true)

    /**
     * Update arara's configuration with the command line arguments.
     */
    private fun updateConfigurationFromCommandLine() {
        Session.userInterfaceOptions = UserInterfaceOptions(
                locale = language?.let { Locale.forLanguageTag(it) }
                        ?: Session.userInterfaceOptions.locale,
                swingLookAndFeel = Session.userInterfaceOptions.swingLookAndFeel
        )
        LanguageController.setLocale(Session.userInterfaceOptions.locale)

        Executor.executionOptions = ExecutionOptions
                .from(Executor.executionOptions)
                .copy(
                        maxLoops = maxLoops
                                ?: Executor.executionOptions.maxLoops,
                        timeoutValue = timeout?.milliseconds
                                ?: Executor.executionOptions.timeoutValue,
                        verbose = if (verbose)
                            true
                        else
                            Executor.executionOptions.verbose,
                        executionMode = if (dryRun)
                            ExecutionMode.DRY_RUN
                        else
                            Executor.executionOptions.executionMode,
                        parseOnlyHeader = if (onlyHeader)
                            true
                        else
                            Executor.executionOptions.parseOnlyHeader
                )

        Session.loggingOptions = LoggingOptions(
                enableLogging = if (log)
                    true
                else
                    Session.loggingOptions.enableLogging,
                appendLog = Session.loggingOptions.appendLog,
                logFile = Session.loggingOptions.logFile
        )
    }

    /**
     * The actual main method of arara (when run in command-line mode)
     */
    override fun run() {
        // the first component to be initialized is the
        // logging controller; note init() actually disables
        // the logging, so early exceptions won't generate
        // a lot of noise in the terminal
        LoggingUtils.init()

        // arara features a stopwatch, so we can see how much time has passed
        // since everything started; internally, this class makes use of
        // nano time, so we might get an interesting precision here
        // (although timing is not a serious business in here, it's
        // just a cool addition)
        val executionStart = TimeSource.Monotonic.markNow()

        // logging has to be initialized only once and for all because
        // context resets lead to missing output
        LoggingUtils.setupLogging(LoggingOptions(log))

        // resolve the working directory from the one that may be given
        // as command line parameter
        val workingDir = FileHandling.normalize(
                workingDirectory
                ?: Arara.currentProject.workingDirectory
        )

        // add all command line call parameters to the session
        parameters.forEach { (key, value) -> Session.put("arg:$key", value) }

        try {
            val projects = listOf(Project(
                    workingDir.fileName.toString(),
                    workingDir,
                    reference.map { fileName ->
                        FileSearching.resolveFile(
                                fileName,
                                workingDir,
                                Executor.executionOptions
                        ).let {
                            if (it.path.isAbsolute)
                                it
                            else
                                ProjectFile(
                                        workingDir.resolve(it.path),
                                        it.fileType,
                                        it.priority
                                )
                        }
                    }.toSet()
            ))
            try {
                Executor.hooks = ExecutorHooks(
                        executeBeforeExecution = {
                            // directive processing has to be initialized, so that the core
                            // component respects our MVEL processing
                            DirectiveUtils.initializeDirectiveCore()
                        },
                        executeBeforeProject = { project ->
                            Arara.currentProject = project
                            ConfigurationUtils.configFile?.let {
                                DisplayUtils.configurationFileName = it.toString()
                                ConfigurationUtils.load(it)
                            }
                        },
                        executeBeforeFile = {
                            // TODO: do we have to reset some more file-specific config?
                            // especially the working directory will have to be set and
                            // changed
                            updateConfigurationFromCommandLine()
                            Arara.currentFile = it
                            DisplayUtils.printFileInformation()
                        },
                        executeAfterFile = {
                            // add an empty line between file executions
                            println()
                        },
                        processDirectives = {
                            DirectiveUtils.process(it)
                        }
                )
                Executor.executionStatus = if (Executor.execute(projects).exitCode != 0)
                    ExecutionStatus.EXTERNAL_CALL_FAILED
                else
                    ExecutionStatus.PROCESSING
            } catch (exception: AraraException) {
                // something bad just happened, so arara will print the proper
                // exception and provide details on it, if available; the idea
                // here is to propagate an exception throughout the whole
                // application and catch it here instead of a local treatment
                DisplayUtils.printException(exception)
            }

            // this is the last command from arara; once the execution time is
            // available, print it; note that this notification is suppressed
            // when the command line parsing returns false as result (it makes
            // no sense to print the execution time for a help message, I guess)
            DisplayUtils.printTime(executionStart.elapsedNow().inSeconds)
        } catch (ex: AraraException) {
            DisplayUtils.printException(ex)
            Executor.executionStatus = ExecutionStatus.CAUGHT_EXCEPTION
        }

        throw ProgramResult(Executor.executionStatus.exitCode)
    }
}