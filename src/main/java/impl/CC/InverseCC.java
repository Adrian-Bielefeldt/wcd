package impl.CC;

import static utility.SC.violation_statement_query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.log4j.Logger;
import org.semanticweb.vlog4j.core.model.api.Atom;
import org.semanticweb.vlog4j.core.reasoner.exceptions.ReasonerStateException;

import impl.PCC.InversePCC;
import impl.PCC.PropertyConstraintChecker;
import main.Main;
import utility.InequalityHelper;
import utility.Utility;

public class InverseCC extends ConstraintChecker {
	
	final static Logger logger = Logger.getLogger(ItemRequiresStatementCC.class);
	
	public static final String REQUIRED_PROPERTY = "P2306";
	
	Map<String, Set<String>> configuration;

	public InverseCC() throws IOException {
		super("Q21510855");
	}

	@Override
	void initDataField() {
		configuration = new HashMap<String, Set<String>>();
	}
	
	@Override
	protected Set<Atom> queries() {
		return asSet(violation_statement_query);
	}
	
	@Override
	protected Set<String> qualifiers() {
		return asSet();
	}

	@Override
	protected Set<String> concatQualifiers() {
		return asSet(REQUIRED_PROPERTY);
	}

	@Override
	protected void process(QuerySolution solution) {
		String property = Utility.addBaseURI(solution.get("item").asResource().getLocalName());
		
		if (!configuration.containsKey(property))
			configuration.put(property, new HashSet<String>());
		
		RDFNode node = solution.get(REQUIRED_PROPERTY);
		if (node.isLiteral()) {
			Literal literal = node.asLiteral();
			String content = literal.getString();
			if (content.equals(""))
				return;
			for (String value : literal.getString().split(",")) {
				configuration.get(property).add(value);				
			}
		} else {
			logger.error("Node " + node + " is no a literal.");
		}
	}

	@Override
	void prepareFacts() throws ReasonerStateException, IOException {
		Main.statementSet.loadFirstFile(reasoner);
		Main.statementSet.loadNextFile(reasoner);
		Main.statementSet.loadLastFile(reasoner);
	}
	
	@Override
	public void registerInequalities() throws IOException {
		Set<String> properties = new HashSet<String>();
		for (Map.Entry<String, Set<String>> entry : configuration.entrySet()) {
			properties.addAll(entry.getValue());
		}
		InequalityHelper.getInequalityHelper(this)
		.registerInequality(properties)
		.registerInequality(Main.statementSet.getStatementFile(), 1)
		.registerInequality(Main.statementSet.getStatementFile(), 2);
	}
	
	@Override
	protected List<PropertyConstraintChecker> propertyCheckers() throws IOException {
		List<PropertyConstraintChecker> result = new ArrayList<PropertyConstraintChecker>();
		for(Map.Entry<String, Set<String>> entry : configuration.entrySet()) {
			result.add(new InversePCC(entry.getKey(), entry.getValue()));
		}
		return result;
	}
}
