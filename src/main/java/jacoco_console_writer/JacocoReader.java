package jacoco_console_writer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.analysis.IMethodCoverage;
import org.jacoco.core.analysis.IPackageCoverage;
import org.jacoco.core.analysis.ISourceFileCoverage;
import org.jacoco.core.data.ExecutionDataReader;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.IncompatibleExecDataVersionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hu.sed.soda.data.CoverageMatrix;
import hu.sed.soda.data.ResultsMatrix;
import hu.sed.soda.data.ResultsMatrix.TestResultType;

public class JacocoReader {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(JacocoReader.class);
	
	private static final String PATH_SEPARATOR = ".";
	private static final String LINE_INFO_SEPARATOR = "-";
	private static final CoverageMatrix COVERAGE_MATRIX = new CoverageMatrix();
	private static final ResultsMatrix RESULTS_MATRIX = new ResultsMatrix();

	private final File executionDataFile;
	private final File classesDirectory;
	private final Granularity coverageType;
	
	public static void saveMatrices(final String coverageFile, final String resultsFile) {
		COVERAGE_MATRIX.save(coverageFile);
		COVERAGE_MATRIX.dispose();
		RESULTS_MATRIX.save(resultsFile);
		RESULTS_MATRIX.dispose();
	}
	
	public JacocoReader(final File projectDirectory, final Granularity coverageType) {
		this.executionDataFile = new File(projectDirectory, "target/jacoco.exec");
		this.classesDirectory = new File(projectDirectory, "target/classes");
		this.coverageType = coverageType;
		RESULTS_MATRIX.addRevisionNumber(0);
	}
	
	public File getExecutionDataFile() {
		return executionDataFile;
	}

	public File getClassesDirectory() {
		return classesDirectory;
	}

	public Granularity getCoverageType() {
		return coverageType;
	}
	
	public void fillMatrices() throws IOException {
		final Visitor visitor = readExecutionData();
		int i = 0;
		final List<String> coverageList = new ArrayList<String>();
		for (Map.Entry<String, ExecutionDataStore> item : visitor.getSessions().entrySet()) {
			if(!("".equals(item.getKey()))) {
				final String testCaseName = getTestCaseNameWithoutResult(item.getKey());
				addTestCaseNameToMatrices(testCaseName);
				createReportFromBundleCoverage(item, coverageList);
				addCodeElementsToCoverageMatrix(coverageList);
				refitMatricesSize();
				final TestResultType resultType = getTestResultType(item.getKey());
				RESULTS_MATRIX.setResult(0, testCaseName, resultType);
				setRelationsInCoverageMatrix(coverageList, testCaseName);
				coverageList.clear();
				LOGGER.info(String.format("%s. Test Case Done: %s", ++i, item.getKey()));
			}
		}
	}

	private Visitor readExecutionData() throws FileNotFoundException, IOException, IncompatibleExecDataVersionException {
		final FileInputStream in = new FileInputStream(executionDataFile);
		final ExecutionDataReader reader = new ExecutionDataReader(in);
		final Visitor visitor = new Visitor();
		reader.setSessionInfoVisitor(visitor);
		reader.setExecutionDataVisitor(visitor);
		reader.read();
		in.close();
		return visitor;
	}

	private String getTestCaseNameWithoutResult(final String key) {
		return key.substring(0, key.length() - 5);
	}

	private void addTestCaseNameToMatrices(final String testCaseName) {
		COVERAGE_MATRIX.addTestcaseName(testCaseName);
		RESULTS_MATRIX.addTestcaseName(testCaseName);
	}

	private void createReportFromBundleCoverage(final Map.Entry<String, ExecutionDataStore> item, final List<String> coverageList)
			throws IOException {
		IBundleCoverage bundleCoverage = analyzeStructure(item.getValue(), item.getKey());
		packageReport(bundleCoverage, coverageList);
	}

	private IBundleCoverage analyzeStructure(final ExecutionDataStore dataStore, final String title) throws IOException {
		final CoverageBuilder coverageBuilder = new CoverageBuilder();
		final Analyzer analyzer = new Analyzer(dataStore, coverageBuilder);

		analyzer.analyzeAll(classesDirectory);

		return coverageBuilder.getBundle(title);
	}
	
	private void packageReport(final IBundleCoverage bundleCoverage, final List<String> coverageList) {
		for (IPackageCoverage packageCoverage : bundleCoverage.getPackages()) {
			if(coverageType.equals(Granularity.PACKAGE)) {
				if(isPackageCovered(packageCoverage)) {
					coverageList.add(packageCoverage.getName());
				}
			} else if (coverageType.equals(Granularity.CLASS) || coverageType.equals(Granularity.METHOD)) {
				classReport(coverageList, packageCoverage);
			} else {
				sourcefileReport(coverageList, packageCoverage);
			}
		}
	}

	private boolean isPackageCovered(final IPackageCoverage packageCoverage) {
		return packageCoverage.getClassCounter().getStatus() == ICounter.FULLY_COVERED || packageCoverage.getClassCounter().getStatus() == ICounter.PARTLY_COVERED;
	}
	
	private void classReport(final List<String> coverageList, final IPackageCoverage packageCoverage) {
		for(IClassCoverage classCoverage : packageCoverage.getClasses()) {
			if(coverageType.equals(Granularity.CLASS)) {
				if(isClassCovered(classCoverage)) {
					coverageList.add(classCoverage.getName());
				}
			} else {
				methodReport(coverageList, classCoverage);
			}
		}
	}

	private boolean isClassCovered(final IClassCoverage classCoverage) {
		return classCoverage.getClassCounter().getStatus() == ICounter.FULLY_COVERED || classCoverage.getClassCounter().getStatus() == ICounter.PARTLY_COVERED;
	}
	
	private void methodReport(final List<String> coverageList, final IClassCoverage classCoverage) {
		for(IMethodCoverage methodCoverage : classCoverage.getMethods()) {
			if(isMethodCovered(methodCoverage)) {
				coverageList.add(String.format("%s%s%s%s", classCoverage.getName(), PATH_SEPARATOR, methodCoverage.getName(), methodCoverage.getDesc()));
			}
		}
	}

	private boolean isMethodCovered(final IMethodCoverage methodCoverage) {
		return methodCoverage.getMethodCounter().getStatus() == ICounter.FULLY_COVERED || methodCoverage.getMethodCounter().getStatus() == ICounter.PARTLY_COVERED;
	}
	
	private void sourcefileReport(final List<String> coverageList, final IPackageCoverage packageCoverage) {
		for (ISourceFileCoverage sourcefileCoverage : packageCoverage.getSourceFiles()) {
			for(int line = 1; line <= sourcefileCoverage.getLastLine(); line++) {
				if(isLineCovered(sourcefileCoverage, line)) {
					coverageList.add(String.format("%s%s%s%s%s", sourcefileCoverage.getPackageName(), PATH_SEPARATOR, sourcefileCoverage.getName(), LINE_INFO_SEPARATOR, line));
				}
			}
		}
	}

	private boolean isLineCovered(final ISourceFileCoverage sourcefileCoverage, final int line) {
		return sourcefileCoverage.getLine(line).getStatus() == ICounter.FULLY_COVERED || sourcefileCoverage.getLine(line).getStatus() == ICounter.PARTLY_COVERED;
	}

	private void addCodeElementsToCoverageMatrix(final List<String> coverageList) {
		for(String coverage : coverageList) {
			COVERAGE_MATRIX.addCodeElementName(coverage);
		}
	}

	private void refitMatricesSize() {
		COVERAGE_MATRIX.refitMatrixSize();
		RESULTS_MATRIX.refitMatrixSize();
	}

	private TestResultType getTestResultType(final String key) {
		if(key.endsWith("PASS")) {
			return TestResultType.Passed;
		} else if(key.endsWith("FAIL")) {
			return TestResultType.Failed;
		}
		return TestResultType.NotExecuted;
	}
	
	private void setRelationsInCoverageMatrix(final List<String> coverageList, final String testCaseName) {
		for(String coverage : coverageList) {
			COVERAGE_MATRIX.setRelation(testCaseName, coverage, true);
		}
	}
}
