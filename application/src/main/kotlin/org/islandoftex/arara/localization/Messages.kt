// SPDX-License-Identifier: BSD-3-Clause

package org.islandoftex.arara.localization

import ch.qos.cal10n.BaseName
import ch.qos.cal10n.Locale
import ch.qos.cal10n.LocaleData

/**
 * This enumeration contains all application messages.
 *
 * @author Paulo Roberto Massa Cereda
 * @version 4.0
 * @since 4.0
 */
@BaseName("org.islandoftex.arara.localization.messages")
@LocaleData(Locale(value = "de", charset = "UTF-8"),
        Locale(value = "en", charset = "UTF-8"),
        Locale(value = "en_QN", charset = "UTF-8"),
        Locale(value = "it", charset = "UTF-8"),
        Locale(value = "nl", charset = "UTF-8"),
        Locale(value = "pt_BR", charset = "UTF-8"))
enum class Messages {
    ERROR_BASENAME_NOT_A_FILE,
    ERROR_CALCULATEHASH_IO_EXCEPTION,
    ERROR_CHECKBOOLEAN_NOT_VALID_BOOLEAN,
    ERROR_CHECKOS_INVALID_OPERATING_SYSTEM,
    ERROR_CHECKREGEX_IO_EXCEPTION,
    ERROR_CONFIGURATION_GENERIC_ERROR,
    ERROR_CONFIGURATION_LOOPS_INVALID_RANGE,
    ERROR_DISCOVERFILE_FILE_NOT_FOUND,
    ERROR_EVALUATE_COMPILATION_FAILED,
    ERROR_EVALUATE_NOT_BOOLEAN_VALUE,
    ERROR_EXTRACTOR_IO_ERROR,
    ERROR_FILETYPE_NOT_A_FILE,
    ERROR_FILETYPE_UNKNOWN_EXTENSION,
    ERROR_GETAPPLICATIONPATH_ENCODING_EXCEPTION,
    ERROR_GETCANONICALFILE_IO_EXCEPTION,
    ERROR_GETPARENTCANONICALPATH_IO_EXCEPTION,
    ERROR_INTERPRETER_ARGUMENT_IS_REQUIRED,
    ERROR_INTERPRETER_COMMAND_RUNTIME_ERROR,
    ERROR_INTERPRETER_DEFAULT_VALUE_RUNTIME_ERROR,
    ERROR_INTERPRETER_EXIT_RUNTIME_ERROR,
    ERROR_INTERPRETER_FLAG_RUNTIME_EXCEPTION,
    ERROR_INTERPRETER_RULE_NOT_FOUND,
    ERROR_INTERPRETER_UNKNOWN_KEYS,
    ERROR_INTERPRETER_WRONG_EXIT_CLOSURE_RETURN,
    ERROR_ISSUBDIRECTORY_NOT_A_DIRECTORY,
    ERROR_LANGUAGE_INVALID_CODE,
    ERROR_LOAD_COULD_NOT_LOAD_XML,
    ERROR_PARSER_INVALID_PREAMBLE,
    ERROR_PARSERULE_GENERIC_ERROR,
    ERROR_REPLICATELIST_MISSING_FORMAT_ARGUMENTS_EXCEPTION,
    ERROR_RULE_IDENTIFIER_AND_PATH,
    ERROR_RUN_GENERIC_EXCEPTION,
    ERROR_RUN_INTERRUPTED_EXCEPTION,
    ERROR_RUN_INVALID_EXIT_VALUE_EXCEPTION,
    ERROR_RUN_IO_EXCEPTION,
    ERROR_RUN_TIMEOUT_EXCEPTION,
    ERROR_RUN_TIMEOUT_INVALID_RANGE,
    ERROR_SAVE_COULD_NOT_SAVE_XML,
    ERROR_SESSION_OBTAIN_UNKNOWN_KEY,
    ERROR_SESSION_REMOVE_UNKNOWN_KEY,
    ERROR_VALIDATE_EMPTY_FILES_LIST,
    ERROR_VALIDATE_FILES_IS_NOT_A_LIST,
    ERROR_VALIDATE_INVALID_DIRECTIVE_FORMAT,
    ERROR_VALIDATE_NO_DIRECTIVES_FOUND,
    ERROR_VALIDATE_ORPHAN_LINEBREAK,
    ERROR_VALIDATE_REFERENCE_IS_RESERVED,
    ERROR_VALIDATE_YAML_EXCEPTION,
    ERROR_VALIDATEBODY_ARGUMENT_ID_IS_RESERVED,
    ERROR_VALIDATEBODY_DUPLICATE_ARGUMENT_IDENTIFIERS,
    ERROR_VALIDATEBODY_MISSING_KEYS,
    ERROR_VALIDATEBODY_NULL_ARGUMENT_ID,
    ERROR_VALIDATEBODY_NULL_COMMAND,
    ERROR_VALIDATEHEADER_NULL_ID,
    ERROR_VALIDATEHEADER_NULL_NAME,
    ERROR_VALIDATEHEADER_WRONG_IDENTIFIER,
    INFO_DISPLAY_EXCEPTION_MORE_DETAILS,
    INFO_DISPLAY_EXECUTION_TIME,
    INFO_DISPLAY_FILE_INFORMATION,
    INFO_INTERPRETER_DRYRUN_MODE_BOOLEAN_MODE,
    INFO_INTERPRETER_DRYRUN_MODE_SYSTEM_COMMAND,
    INFO_LABEL_AUTHOR,
    INFO_LABEL_AUTHORS,
    INFO_LABEL_CONDITIONAL,
    INFO_LABEL_NO_AUTHORS,
    INFO_LABEL_ON_DETAILS,
    INFO_LABEL_ON_ERROR,
    INFO_LABEL_ON_FAILURE,
    INFO_LABEL_ON_SUCCESS,
    INFO_LABEL_UNNAMED_TASK,
    INFO_PARSER_ALL_RIGHTS_RESERVED,
    INFO_PARSER_NOTES,
    LOG_INFO_BEGIN_BUFFER,
    LOG_INFO_BOOLEAN_MODE,
    LOG_INFO_DIRECTIVES_BLOCK,
    LOG_INFO_END_BUFFER,
    LOG_INFO_INTERPRET_RULE,
    LOG_INFO_INTERPRET_TASK,
    LOG_INFO_POTENTIAL_DIRECTIVE_FOUND,
    LOG_INFO_POTENTIAL_PATTERN_FOUND,
    LOG_INFO_RULE_LOCATION,
    LOG_INFO_SYSTEM_COMMAND,
    LOG_INFO_TASK_RESULT,
    LOG_INFO_VALIDATED_DIRECTIVES,
    LOG_INFO_WELCOME_MESSAGE
}