package utility;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.semanticweb.vlog4j.core.model.api.Atom;
import org.semanticweb.vlog4j.core.model.api.Constant;
import org.semanticweb.vlog4j.core.model.api.Predicate;
import org.semanticweb.vlog4j.core.model.api.Rule;
import org.semanticweb.vlog4j.core.model.api.Term;
import org.semanticweb.vlog4j.core.model.api.Variable;
import org.semanticweb.vlog4j.core.model.implementation.Expressions;
import org.semanticweb.vlog4j.core.reasoner.Reasoner;
import org.semanticweb.vlog4j.core.reasoner.exceptions.ReasonerStateException;

import impl.TS.TripleSetFile;

import static utility.SC.require_inequality;

public class InequalityHelper {
	
	public enum Mode {
		NAIVE,
		ENCODED,
		DEMANDED
	}
	
	final static int chunkSize = 7;
	
	final static String NONE = "none";
	final static String EMPTY = "empty";
	
	final static String X = "x";
	final static String Y = "y";
	
	final static String CON1 = "con1";
	final static String CON2 = "con2";
	
	final static String UNEQUAL_EDB = "unequal_EDB";
	final static String UNEQUAL = "unequal";
	
	final static Variable x = Expressions.makeVariable(X);
	final static Variable y = Expressions.makeVariable(Y);
	
	final static Predicate unequal_EDB = Expressions.makePredicate(UNEQUAL_EDB, 2);
	public final static Predicate unequal = Expressions.makePredicate(UNEQUAL, 2);
	
	// require_inequality(X, Y)
	final static Atom require_inequality_XY = Expressions.makeAtom(require_inequality, x, y);
	
	// unequal(X, Y)
	final static Atom unequal_XY = Expressions.makeAtom(unequal, x, y);
	
	// unequal_IDB(X, Y) :- unequal_EDB(X, Y)
	final static Rule unequal_IDB_EDB = Expressions.makeRule(Expressions.makeAtom(unequal, x, y), Expressions.makeAtom(unequal_EDB, x, y));
			
	// unequal_IDB(X, Y) :- unequal_IDB(Y, X)
	final static Rule inverse = Expressions.makeRule(Expressions.makeAtom(unequal, x, y), Expressions.makeAtom(unequal, y, x));
	
	final static List<Rule> rules = new ArrayList<>();
	
	final static Set<String> characters = new HashSet<String>();
	
	final static List<TripleSetFile> files = new ArrayList<>();
	
	final static Set<InequalityFileIndex> inequalityFileIndexes = new HashSet<>();
	
	final static Set<String> additionalInequalities = new HashSet<String>();
	
	public static Mode mode = Mode.ENCODED;
	
	public static void reset() {
		characters.clear();
		files.clear();
		inequalityFileIndexes.clear();
		additionalInequalities.clear();
	} 
	
	public static void registerInequality(Set<String> inequalities) {
		additionalInequalities.addAll(inequalities);
	}
	
	public static void registerInequality(File file, int index) {
		inequalityFileIndexes.add(new InequalityFileIndex(file, index));
	}
	
	public static void prepareFiles() throws IOException {
		switch (mode) {
		case NAIVE:
			naive();
			break;
		case ENCODED:
			encoded(false);
			break;
		case DEMANDED:
			encoded(true);
			break;
		}
	}
	
	public static void load(Reasoner reasoner) throws ReasonerStateException, IOException {
		reasoner.addRules(unequal_IDB_EDB, inverse);
		
		reasoner.addRules(rules);
		
		List<Atom> facts = allCharactersUnequal(characters);
		reasoner.addFacts(facts);
		
		for (TripleSetFile tripleSetFile : files) {
			tripleSetFile.loadFile(reasoner);
		}
	}

	static void naive () throws IOException {
		TripleSetFile file = new TripleSetFile("unequal_naive", unequal_EDB); 
		files.add(file);

		List<String> fixedOrderValues = new ArrayList<String>(additionalInequalities);
		
		List<Iterator<String>> firstIterators = new ArrayList<>();
		for (InequalityFileIndex inequalityFile : inequalityFileIndexes) {
			firstIterators.add(inequalityFile.getIterator());
		}
		firstIterators.add(fixedOrderValues.iterator());
		
		Iterator<String> firstIterator = new CombinedIterator<>(firstIterators);
		
		int first = 0;
		while(firstIterator.hasNext()) {
			String firstEntry = firstIterator.next();
			
			List<Iterator<String>> secondIterators = new ArrayList<>();
			for (InequalityFileIndex inequalityFile : inequalityFileIndexes) {
				secondIterators.add(inequalityFile.getIterator());
			}
			secondIterators.add(fixedOrderValues.iterator());
			
			Iterator<String> secondIterator = new CombinedIterator<>(secondIterators);
			
			for (int second = 0; second <= first; second++) {
				if (secondIterator.hasNext())
					secondIterator.next();
			}
			
			while (secondIterator.hasNext()) {
				String secondEntry = secondIterator.next();
				if (secondEntry.equals(firstEntry))
					continue;
				
				file.write(firstEntry, secondEntry);
			}
			
			first++;
		}
	}

	static void encoded(boolean demand) throws IOException {
		List<Iterator<String>> iterators = new ArrayList<>();
		for (InequalityFileIndex inequalityFile : inequalityFileIndexes) {
			iterators.add(inequalityFile.getIterator());
		}
		iterators.add(additionalInequalities.iterator());
		
		Iterator<String> iterator = new CombinedIterator<>(iterators);
		
		while (iterator.hasNext()) {
			String string = iterator.next();
			int i = 0;
			for (List<String> chunk : chunks(string)) {
				characters.addAll(chunk);
				
				TripleSetFile file = getChunkFile(i, demand);
				
				List<String> line = new ArrayList<>();
				line.add(string);
				line.addAll(chunk);
				file.write(line);
				i++;
			}
		}
	}
	
	static TripleSetFile getChunkFile(int chunk, boolean demand) throws IOException {
		if (chunk < files.size())
			return files.get(chunk);
	
		int chunkAdress = chunk * chunkSize;
		String LETTER_I = "letter" + chunkAdress;
		Predicate letter_i = Expressions.makePredicate(LETTER_I, chunkSize + 1);
		TripleSetFile file = new TripleSetFile(LETTER_I, letter_i);
		file.forceWrite();
		files.add(file);
		
		List<Atom> inequalities = new ArrayList<>();
		
		// letter_i(X, Ai, Ai+1, ..., Ai+14)
		List<Term> xVariables = new ArrayList<>();
		xVariables.add(x);
		// letter_i(Y, Bi, Bi+1, ..., Bi+14)
		List<Term> yVariables = new ArrayList<>();
		yVariables.add(y);
		for (int j = chunkAdress; j <= chunkAdress + chunkSize - 1; j++) {
			Variable ai = Expressions.makeVariable("a" + j);
			Variable bi = Expressions.makeVariable("b" + j);
			xVariables.add(ai);
			yVariables.add(bi);
			
			// unequal(Ai, Bi)
			Atom unequal_AB = Expressions.makeAtom(unequal, ai, bi);
			inequalities.add(unequal_AB);
		}
		
		Atom letter_i_XA = Expressions.makeAtom(letter_i, xVariables);
		Atom letter_i_YB = Expressions.makeAtom(letter_i, yVariables);
		
		for (Atom inequality : inequalities) {
			List<Atom> body = new ArrayList<>();
			if (demand)
				body.add(require_inequality_XY);
			body.add(letter_i_XA);
			body.add(letter_i_YB);
			body.add(inequality);

			Rule unequal = Expressions.makeRule(unequal_XY, body.toArray(new Atom[body.size()]));
			rules.add(unequal);
		}
		
		return file;
	}
	
	static List<List<String>> chunks (String string) {
		List<List<String>> result = new ArrayList<>();
		
		List<String> list = toList(string);
		list.add(NONE);

		for (int i = 0; i < list.size(); i += chunkSize) {
			List<String> part = new ArrayList<>();
			for (int j = i; j < i + chunkSize; j++) {
				if (list.size() <= j)
					break;
				String toAdd = list.get(j);
				if (toAdd.equals(" "))
					toAdd = EMPTY;
				part.add(toAdd);
			}
			fillUp(part);
			result.add(part);
		}
		return result;
	}
	
	static List<String> toList(String string) {
		List<String> result = new ArrayList<>();
		for (int i = 0; i < string.length(); i++) {
			result.add(string.substring(i, i+1));
		}
		return result;
	}
	
	static void fillUp(List<String> list) {
		for (int i = list.size(); i < chunkSize; i++) {
			list.add(NONE);
		}
	}

	static List<Atom> allCharactersUnequal(Set<String> characters) {
		List<Atom> atoms = new ArrayList<Atom>();
		
		List<Constant> indentifierParts = new ArrayList<Constant>();
		for (String allowedCharacter : characters) {
			indentifierParts.add(Expressions.makeConstant(allowedCharacter));
		}
		
		for (int first = 0; first < indentifierParts.size(); first++) {
			Constant firstConstant = indentifierParts.get(first);
			for (int second = first + 1; second < indentifierParts.size(); second++) {
				Constant secondConstant = indentifierParts.get(second);
				
				atoms.add(Expressions.makeAtom(unequal_EDB, firstConstant, secondConstant));
			}
		}
		
		return atoms;
	}

	public static void delete() {
		// TODO Auto-generated method stub
		
	}
	
	
}
