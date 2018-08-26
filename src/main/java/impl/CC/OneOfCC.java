package impl.CC;

import static utility.SC.violation_qualifier_query;
import static utility.SC.violation_reference_query;
import static utility.SC.violation_triple_query;

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

import impl.PCC.OneOfPCC;
import impl.PCC.PropertyConstraintChecker;
import main.Main;
import utility.InequalityHelper;
import utility.Utility;

public class OneOfCC extends ConstraintChecker {
	
	final static Logger logger = Logger.getLogger(OneOfCC.class);
	
	public static final String ALLOWED_VALUES = "P2305";
	
	Map<String, HashSet<String>> allowedValues;

	public OneOfCC() throws IOException {
		super("Q21510859");
	}

	@Override
	void initDataField() {
		allowedValues = new HashMap<String, HashSet<String>>();
	}
	
	@Override
	protected Set<String> qualifiers() {
		return asSet();
	}

	@Override
	protected Set<String> concatQualifiers() {
		return asSet(ALLOWED_VALUES);
	}

	@Override
	protected void process(QuerySolution solution) {
		String property = Utility.addBaseURI(solution.get("item").asResource().getLocalName());
		
		if (!allowedValues.containsKey(property))
			allowedValues.put(property, new HashSet<String>());

		RDFNode node = solution.get(ALLOWED_VALUES);
		if (node.isLiteral()) {
			Literal literal = node.asLiteral();
			String content = literal.getString();
			if (content.equals(""))
				return;
			for (String value : literal.getString().split(",")) {
				allowedValues.get(property).add(value);				
			}
		} else {
			logger.error("Node " + node + " is no a literal.");
		}
	}
	
	@Override
	protected Set<Atom> queries() {
		return asSet(violation_triple_query, violation_qualifier_query, violation_reference_query);
	}

	@Override
	void prepareFacts() throws ReasonerStateException, IOException {
	}
	
	@Override
	public void registerInequalities() throws IOException {
		Set<String> values = new HashSet<String>();
		for (Set<String> valuesSet : allowedValues.values()) {
			values.addAll(valuesSet);
		}
		InequalityHelper.registerInequality(values);
		InequalityHelper.registerInequality(Main.tripleSet.getTripleFile(), 3);
		InequalityHelper.registerInequality(Main.tripleSet.getQualifierFile(), 2);
		InequalityHelper.registerInequality(Main.tripleSet.getReferenceFile(), 2);
	}

	@Override
	protected List<PropertyConstraintChecker> propertyCheckers() throws IOException {
		List<PropertyConstraintChecker> result = new ArrayList<PropertyConstraintChecker>();
		for (Map.Entry<String, HashSet<String>> entry : allowedValues.entrySet()) {
			result.add(new OneOfPCC(entry.getKey(), entry.getValue()));
		}
		return result;
	}
}
