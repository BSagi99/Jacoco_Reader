package jacoco_console_writer;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.IExecutionDataVisitor;
import org.jacoco.core.data.ISessionInfoVisitor;
import org.jacoco.core.data.SessionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Visitor implements ISessionInfoVisitor, IExecutionDataVisitor {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Visitor.class);

	private final Map<String, ExecutionDataStore> sessions = new HashMap<String, ExecutionDataStore>();

	private ExecutionDataStore currentStore;

	public void visitClassExecution(final ExecutionData data) {
		currentStore.put(data);
		
		LOGGER.trace(String.format("%016x  %3d of %3d   %s",
				Long.valueOf(data.getId()),
				Integer.valueOf(-1),
				Integer.valueOf(data.getProbes().length),
				data.getName()));
	}

	public void visitSessionInfo(final SessionInfo info) {
		final String sessionId = info.getId();
		
		LOGGER.trace(String.format("Session \"%s\": %s - %s%n",
				sessionId,
				new Date(info.getStartTimeStamp()),
				new Date(info.getDumpTimeStamp())));

		currentStore = sessions.get(sessionId);

		if (currentStore == null) {
			currentStore = new ExecutionDataStore();

			sessions.put(sessionId, currentStore);
		}
	}

	public Map<String, ExecutionDataStore> getSessions() {
		return sessions;
	}

}
