package wikidata.constraints.datalog.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import wikidata.constraints.datalog.impl.PCC.ScopePCC;
import wikidata.constraints.datalog.main.ConstraintChecker;
import wikidata.constraints.datalog.main.PropertyConstraintChecker;

public class ScopeConstraintChecker extends ConstraintChecker {
	
	final static Logger logger = Logger.getLogger(ScopeConstraintChecker.class);
	
	public final static String SCOPE = "scope";
	
	public static final String AS_MAIN_VALUE = "Q54828448";
	public static final String AS_QUALIFIER = "Q54828449";
	public static final String AS_REFERENCE = "Q54828450";

	public ScopeConstraintChecker() throws IOException {
		super("Q53869507");
	}

	@Override
	protected PropertyConstraintChecker getPropertyChecker(String property, Map<String, String> qualifiers) throws IOException {
		return new ScopePCC(property, qualifiers);
	}

	@Override
	protected Map<String, String> additionalQualifiers() {
		Map<String, String> result = new HashMap<String, String>();
		result.put(SCOPE, "P5314");
		return result;
	}
}