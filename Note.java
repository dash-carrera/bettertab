package bettertab;


import java.text.ParseException;
import java.util.ArrayList;

/*
 * This class defines a Note, and some of the basic functions of a note.
 */


class Note {

	int[] positions = new int[6]; //All the possible places the note can be played on the guitar
	 int testIndice = -1; //The current indice we are testing 
	 String decision; //The final decision for the final tab, as a string
	 boolean zero = false; //If the note can be expressed as an open note is true
	 boolean special = false; //True if any modifiers/specials are used
	 ArrayList<Character> specials = new ArrayList<Character>(); //Holds all the modifiers, if applicable
	 ArrayList<Integer> relativePositions = new ArrayList<Integer>(); //Holds the relative position of all notes to priamry note
	 int numPos = 0;
	 
	protected Note(char[] note, int numString) throws ParseException
	//Constructs a note based on the piece of tab that is the note and the string it is found on
	{
		int numQueue = -1;
		ArrayList<Integer> nums = new ArrayList<Integer>();
		for (char piece: note)
		{
			if (optimal_finder.inLegend(piece))
			{
				nums.add(numQueue);
				numQueue = -1;
				specials.add(piece);
				special = true;
			}
			else if (Character.isDigit(piece))
			{
				if (numQueue == -1)
					numQueue = Character.getNumericValue(piece);
				else if (numQueue < 10)
					numQueue = numQueue*10 + Character.getNumericValue(piece);
				else 
					throw new ParseException("Unsupported: 3 or more digit fret.", 0);
			}			
			else
				throw new ParseException("Unknown symbol: " + piece,0);//0 placed to parameters - is insignificant
		}
		if (numQueue != -1)
			nums.add(numQueue);

		int primary = nums.get(0).intValue();

		for (Integer num: nums)
			if (num.intValue() < primary)
				primary = num.intValue();

		for (Integer num: nums)
			relativePositions.add(num.intValue()-primary);

		positions = findPositions(primary, numString);

		if (special && numQueue==-1)
			throw new ParseException("You are misformatting a modifier.",0);

		for (int i = 0; i< specials.size(); i++)
		{
			char option = specials.get(i).charValue();
			int relPos = relativePositions.get(i+1).intValue();
			applyRestrictions(option, relPos);
		}		
		maxCheck(positions);
		numPos = testCount();
		if (numPos == 0)
			throw new ParseException("One note is too high to play given the specified max fret.",0);
	}
	
	protected Note(int[] noteNums, ArrayList<Character> modifiers)
	//Receives notes as "universal note numbers", and modifiers if applicable
	{
		specials = modifiers;
		if (modifiers.size() != 0)
		{
			//for (int note: noteNums)
				
		}
		
				
		
	}
	
	protected void commonInit()
	//Does initialization common to both methods of note construction
	{
		
	}
	
	
	protected void gotoIndice(int indice)
	//Go to the {input}th test indice
	{
		int count = -1; 
		for (int i = 0; i < 6; i++)
		{
			if (positions[i] !=-1)
				count++;
			if (count == indice)
			{
				testIndice = i;
				break;
			}
		}
		assert count!=-1;
	}
	
	protected int testCount()
	{
		numPos = 0;
		for (int i = 0;i < 6; i++)
			if (positions[i] != -1)
				numPos++;
		return numPos;
	}
	
	protected void zeroSet()
	//Sets open string to decision
	{
		zero = true;
		for (int i =0; i < 6; i++)
			if (positions[i] != 0)
				positions[i]=-1;
			else
				testIndice = i;
	}
	
	protected int zeroCheck()
	//If the note can be expressed as an open note, we always want to do it, since open notes can be played from any position
	//Returns the string that the open note can be played on, -1 otherwise
	{
		for (int i =0; i < 6; i++)
			if (positions[i] == 0)
				return i;
		return -1;
	}
	
	
	protected void maxCheck(int[] positions) throws ParseException
	//This removes any  possible note positionings above the maximum fret defined by the user
	{
		int max = optimal_finder.maxFret;
		for (int i =0; i < 6; i++)
			if (positions[i] > max)
				positions[i] = -1;
	}
	
	
	protected void createDecision()
	//This creates a final decision, as a string
	{		
		if (special)
		{
			StringBuffer temp = new StringBuffer();
			int primaryNote = positions[testIndice];
			
			for (int i = 0; i < specials.size(); i++)
			{
				int num = primaryNote + relativePositions.get(i).intValue();
				char spec = specials.get(i).charValue();
				temp.append(Integer.toString(num));
				temp.append(Character.toString(spec));
			}
			int num = primaryNote + relativePositions.get(specials.size()).intValue();
			temp.append(Integer.toString(num));
			decision = temp.toString();
		}
		else
			decision = Integer.toString(positions[testIndice]);
	}
	
	
	protected void applyRestrictions(char whichOption, int difference)
	//Pre: Receives the difference from the primary note that a note is and the special that follows it
	//Post: Applies the restrictions listed so positionings are still valid, accounting for specials
	//This processes any special things (like hammer-ons, etc)
	{	
		if (difference == 0 && (whichOption == optimal_finder.legend[1] || whichOption==optimal_finder.legend[2] || whichOption==optimal_finder.legend[3]))
			for (int i = 0; i < 6; i++)
				if (positions[i] == 0) //Make sure not trying to bend or slide from 0, since that's impossible
					positions[i] = -1;
		if (whichOption != optimal_finder.legend[1]) //Max rule doesn't apply to bends
			for (int i =0; i < 6; i++)
				if (positions[i] + difference > optimal_finder.maxFret) //Make sure secondary none of notes violate max fret rule
					positions[i] = -1;
	}
		
	private int standardTuning (int string)
    { //Returns the universal note number for a string, assuming standard tuning. Lowest note on guitar is 0, second lowest is 1, etc..
	if (string < 4)
	    return string * 5;
	return 19 + (string - 4) * 5;
    }
	
	protected int[] findPositions (int fret, int numString)
	//Pre: Receives a fret and the number string it was found on
	//Post: Returns an array with all the possible fret positions
    { 
	int note = fret + standardTuning (numString);
	for (int i = 0; i < 6; i++)
	{
		int string = standardTuning (i);
		if (note >= string && note <= string + optimal_finder.maxFret)
			positions[i] = note - string;
		else
			positions[i] = -1;
	}
	return positions;
    }	
}