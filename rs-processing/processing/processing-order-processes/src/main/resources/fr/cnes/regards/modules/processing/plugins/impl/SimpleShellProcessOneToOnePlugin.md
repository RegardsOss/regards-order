# How to use the simple shell plugin?

## Order process info

- Limit: 1 input files
- Scope: this plugin has scope ITEM (one execution per feature)
- Cardinality: this plugin has cardinality ONE_PER_INPUT_FILE (each input file generates a single output file) 

## General constraints on the scripts this plugin can launch

This plugin provides a fully customizable way to launch shell scripts.

However, the shell scripts must conform to the following conventions:

- the script must be executable and available in the PATH of the rs-processing instance,
  or be given as an absolute path (in which case the full path must be accessible by the
  java process launching rs-processing)
- the script is invoked directly, with no command line arguments
- all script parameters are set through environment variables, whose names are defined
  in the plugin configuration, and set once and for all at the batch creation
- the script is executed from a specific workdir for each execution, containing:
    + an `input` folder with all the input files for the execution
    + an empty `output` folder where the script must create all the output files
- the script terminates with code 0 in case of success, any other code in case of failure
- the script does not use the standard input
- the script outputs its logs in the standard output
- if the script uses executables, they must be installed, reachable and executable by the process
  launching the rs-processing instance.
  
## Output to Input mapping policy

This plugin attempts to map all its output files to input files having the same name
except for the extension.