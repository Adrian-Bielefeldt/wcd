package impl.PCC;

import static utility.SC.i;
import static utility.SC.item;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.vlog4j.core.model.api.Atom;
import org.semanticweb.vlog4j.core.model.api.Rule;
import org.semanticweb.vlog4j.core.model.implementation.Expressions;

import impl.CC.AllowedEntityTypesCC;
import utility.SC;



public class AllowedEntityTypesPCC extends PropertyConstraintChecker {
	
	final static Logger logger = Logger.getLogger(AllowedEntityTypesPCC.class);
	
	final Set<String> allowedEntityTypes;

	public AllowedEntityTypesPCC(String property_, Set<String> allowedEntityTypes_) throws IOException {
		super(property_);
		allowedEntityTypes = allowedEntityTypes_;
	}

	@Override
	public List<Rule> rules() {
		List<Rule> rules = new ArrayList<Rule>();
		
		// item(I)
		Atom item_I = Expressions.makeAtom(item, i);
		
		// violation_statement(S, I, propertyConstant, V ) :- statementEDB(S, I, propertyConstant, V ), item(I)
		Rule notItem = Expressions.makeRule(violation_statement_SIpV, statementEDB_SIpV, item_I);
		
		// property(I)
		Atom property_I = Expressions.makeAtom(SC.property, i);
		
		// violation_statement(S, I, propertyConstant, V ) :- statementEDB(S, I, propertyConstant, V ), property(I)
		Rule notProperty = Expressions.makeRule(violation_statement_SIpV, statementEDB_SIpV, property_I);
		
		if (!allowedEntityTypes.contains(AllowedEntityTypesCC.AS_ITEM))
			rules.add(notItem);
		
		if (!allowedEntityTypes.contains(AllowedEntityTypesCC.AS_PROPERTY))
			rules.add(notProperty);

		return rules;
	}
}
