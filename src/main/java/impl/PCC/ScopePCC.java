/**
 * 
 */
package impl.PCC;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.vlog4j.core.model.api.Atom;
import org.semanticweb.vlog4j.core.model.api.Rule;
import org.semanticweb.vlog4j.core.model.implementation.Expressions;
import org.semanticweb.vlog4j.core.reasoner.exceptions.ReasonerStateException;

import impl.CC.ScopeCC;
import impl.TS.ScopeTS;
import impl.TS.TripleSet;
import utility.PrepareQueriesException;

/**
 * @author adrian
 *
 */
public class ScopePCC extends PropertyConstraintChecker {
	
	final static Logger logger = Logger.getLogger(ScopePCC.class);
	
	final TripleSet tripleSet;
	
	final Set<String> qualifiers;
	
	public ScopePCC(String property_, Set<String> qualifiers_) throws IOException {
		super(property_);
		qualifiers = qualifiers_;
		tripleSet = new ScopeTS(property);
	}
	
	public String violations() throws IOException {
		if (!tripleSet.notEmpty())
			return "";
		
		try {
			loadTripleSets(tripleSet);
		} catch (ReasonerStateException e) {
			logger.error("Trying to load facts to the reasoner in the wrong state for property " + property + ".", e);
			return internalError;
		}
		
		List<Rule> rules = new ArrayList<Rule>();

		// violation_triple(S, I, propertyConstant, V) :- tripleEDB(S, I, propertyConstant, V)
		Rule notTriple = Expressions.makeRule(violation_triple_SIpV, tripleEDB_SIpV);
		// violation_qualfier(S, propertyConstant, V) :- qualifierEDB(S, propertyConstant, V)
		Rule notQualifier = Expressions.makeRule(violation_qualifier_SpV, qualifierEDB_SpV);
		// violation_reference(S, propertyConstant, V) :- referenceEDB(S, propertyConstant, V)
		Rule notReference = Expressions.makeRule(violation_reference_SpV, referenceEDB_SpV); 
		
		rules.add(notTriple);
		rules.add(notQualifier);
		rules.add(notReference);
		
		List<Atom> queries = new ArrayList<Atom>();
		queries.add(violation_triple_query);
		queries.add(violation_qualifier_query);
		queries.add(violation_reference_query);
		
		if (allowedAs(ScopeCC.AS_MAIN_VALUE)) {
			rules.remove(notTriple);
			queries.remove(violation_triple_query);
		}
		
		if (allowedAs(ScopeCC.AS_QUALIFIER)) {
			rules.remove(notQualifier);
			queries.remove(violation_qualifier_query);
		}
		
		if (allowedAs(ScopeCC.AS_REFERENCE)) {
			rules.remove(notReference);
			queries.remove(violation_reference_query);
		}
		
		try {
			return prepareAndExecuteQueries(rules, queries);
		} catch (PrepareQueriesException e1) {
			return e1.getMessage();
		}
	}
	
	protected boolean allowedAs(String qualifier) {
		boolean result = false;
		for (String allowed : qualifiers) {
			if (allowed.equals(qualifier))
				result = true;
		}
		return result;
	}

	@Override
	protected Set<TripleSet> getRequiredTripleSets() throws IOException {
		return new HashSet<TripleSet>(Arrays.asList(tripleSet));
	}

}
