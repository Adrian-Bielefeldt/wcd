package impl.TS;

import java.io.IOException;
import java.util.Map;

import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;
import org.wikidata.wdtk.datamodel.interfaces.Value;

import utility.OutputValueVisitor;

/**
 * A triple set limited to all direct statements of items with a statement containing the property as predicate.
 * @author adrian
 *
 */
public class ConflictsWithTS extends TripleSet {

	public ConflictsWithTS(String property_) throws IOException {
		super(property_);
	}
	
	@Override
	public void processItemDocument(ItemDocument itemDocument) {
		boolean foundProperty = false;
		
		String subject = itemDocument.getEntityId().getIri();
		for (StatementGroup sg : itemDocument.getStatementGroups()) {
			if (foundProperty)
				break;
			String predicate = sg.getProperty().getIri();
			if (predicate.endsWith(property)) {
				foundProperty = true;
				break;
			}
		}
		if (!foundProperty)
			return;
		for (StatementGroup	sg : itemDocument.getStatementGroups()) {
			String predicate = sg.getProperty().getIri();
			for (Statement statement : sg) {
				String id = statement.getStatementId();
				Value value = statement.getValue();
				String object = "";
				if (value != null) {
					object = value.accept(new OutputValueVisitor());
				}
				triple(id, subject, predicate, object);
			}
		}
	}

	@Override
	protected String getTripleSetType() {
		return "DirectStatementsOnItem";
	}

}
