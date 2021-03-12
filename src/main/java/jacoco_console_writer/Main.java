package jacoco_console_writer;

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class Main {
	
	static {
		// Load the JNI wrapper library.
		System.loadLibrary("SoDAJni");
	}
	
	private static final String DEFAULT_COVERAGE_FILE = "coverageMatrix.SoDA";
	private static final String DEFAULT_RESULTS_FILE = "resultsMatrix.SoDA";
	
	private static Granularity getCoverageType(final String granularity) throws Exception {
		if(granularity.equalsIgnoreCase(Granularity.PACKAGE.getGranularity())) {
			return Granularity.PACKAGE;
		} else if(granularity.equalsIgnoreCase(Granularity.CLASS.getGranularity())) {
			return Granularity.CLASS;
		} else if(granularity.equalsIgnoreCase(Granularity.METHOD.getGranularity())) {
			return Granularity.METHOD;
		} else if(granularity.equalsIgnoreCase(Granularity.LINE.getGranularity())) {
			return Granularity.LINE;
		}
		throw new Exception(String.format("Invalid granularity: %s. Valid granularities are listed below.", granularity));
	}
	
	private static String getCoverageFile(final String coverageFile) {
		if(!("".equals(coverageFile))) {
			if(coverageFile.endsWith(".SoDA")) {
				return coverageFile;
			} else {
				return (coverageFile + ".SoDA");
			}
		}
		return DEFAULT_COVERAGE_FILE;
	}
	
	private static String getResultsFile(final String resultsFile) {
		if(!(resultsFile.equals("")) && resultsFile != null) {
			if(resultsFile.endsWith(".SoDA")) {
				return resultsFile;
			} else {
				return (resultsFile + ".SoDA");
			}
		}
		return DEFAULT_RESULTS_FILE;
	}

	public static void main(String[] args) throws IOException {
		Options options = new Options();
		
		final Option coverage = new Option("c", "coverage", true, "[Optional] Coverage output .SoDA file name (with or without '.SoDA', default is 'coverageMatrix.SoDA'))");
        options.addOption(coverage);
        
        final Option result = new Option("r", "result", true, "[Optional] Result output .SoDA file name (with or without '.SoDA', default is 'resultsMatrix.SoDA')");
        options.addOption(result);
        
        final Option granularity = new Option("g", "granularity", true, "[Required] Valid coverage granularities: package, class, method or line");
        granularity.setRequired(true);
		options.addOption(granularity);
		
		final Option input = new Option("i", "input", true, "[Required] Input project directories that contain .exec and project files (separated with commas)");
		input.setValueSeparator(',');
		input.setRequired(true);
		options.addOption(input);
        
		final CommandLineParser parser = new DefaultParser();
		final HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;
        
        String[] inputs = null;
        Granularity coverageType = null;
        String coverageFile = null;
        String resultsFile = null;
        
        try {
            cmd = parser.parse(options, args);
            inputs = cmd.getOptionValues("input");
            coverageType = getCoverageType(cmd.getOptionValue("granularity"));
            coverageFile = getCoverageFile(cmd.getOptionValue("coverage"));
            resultsFile = getResultsFile(cmd.getOptionValue("result"));
        } catch (Exception e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);
            System.exit(1);
        }
		
		for (int i = 0; i < inputs.length; i++) {
			final JacocoReader jacocoReader = new JacocoReader(
					new File(inputs[i]), coverageType);
			jacocoReader.fillMatrices();
		}
		JacocoReader.saveMatrices(coverageFile, resultsFile);
	}

}
