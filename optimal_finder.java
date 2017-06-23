package bettertab;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

class optimal_finder
{

	protected static int maxFret;
	protected static int fretSpan;
	protected static char[] legend = {'h','b','/','\\','p'};
	protected boolean splitChords = true;
	private long startTime;

	protected optimal_finder()
	{
	}
	
	protected String find(String original_tab, int max, int span, boolean split) throws ParseException, Exception, TimeoutException
	// MAIN METHOD - Receives original tab, returns optimal tab
	{
		// original_tab = "E----------0-------------2-----------------2-----\nB----------0-0---------2-2-2-------------2-2-2---\nG--------------1-----2----------2------2---------\nD------6-9-------2-4---------------4-4-----------\nA-7----------------------------------------------\nE------------------------------------------------\n";
		maxFret = max;
		fretSpan = span;
		splitChords = split;
		startTime = System.currentTimeMillis();
		
		String reverse_tab;
		try
		{
			reverse_tab = reverseTab(original_tab);
		}
		catch (Exception e)
		{
			return "This is not a valid piece of guitar tablature. Check the instructions for more information.";
		}
		
		
		char[][] lineProcessed = process_tab(reverse_tab);
		ArrayList<NoteGroup> timingProcessed = noteProcess(lineProcessed);
		ArrayList<NoteGroup> optimized = optimize(timingProcessed);
		String newTab = finishedTab(optimized);
		return reverseTab(newTab);
	}

	/*
	 * TAB PARSING ***********************************************************
	 */

	private char[][] process_tab(String original_tab)
	// Processes tab into a 2D character array
	{
		char[][] newTab = new char[6][];
		String[] tabString = original_tab.split("\n");
		for (int i = 0; i < 6; i++)
			newTab[i] = tabString[i].toCharArray();
		return newTab;
	}

	private String reverseTab(String original_tab) throws ParseException
	//Flip the tab upside down for more intuitive programming
	{
		String[] tabString = original_tab.split("\n");
		String[] newTab = new String[6];
		for (int i = 0; i < 6; i++)
		{
			if (tabString[i].equals(""))
				throw new ParseException("Make sure all lines contain tablature.",0);
			newTab[5 - i] = tabString[i];
		}
		StringBuffer temp = new StringBuffer(newTab[0]);
		for (int i = 1; i < 6; i++)
			temp.append("\n" + newTab[i]);
		return temp.toString();
	}

	private ArrayList<NoteGroup> noteProcess(char[][] tab) throws ParseException
	// Pre: Receives the tab as a 2D character array
	// Post: Returns an ArrayList of NoteGroups
	{
		ArrayList<NoteGroup> newTab = new ArrayList<NoteGroup>();
		int greatestLength = 0;
		for (int i = 0; i < 6; i++)
			if (tab[i].length > greatestLength)
				greatestLength = tab[i].length;
		int lowerBound = 0;

		boolean[] usedStrings = new boolean[6];
		for (int i = 0; i < 6; i++)
			usedStrings[i] = true;
		boolean[] nextStrings = new boolean[6];
		boolean lookingForEnd = false; // looking for end of a chunk of notes
		boolean hasData = false; // Holds if something was found on a column sweep

		for (int j = 0; j < greatestLength; j++)
		{
			for (int i = 0; i < 6; i++)
				if (tab[i].length > j) // Handles tab ending different lengths
				{
					if (Character.isDigit(tab[i][j]) && !lookingForEnd)
					{
						lowerBound = j; // Set to beginning of note chunk
						lookingForEnd = true;
					}

					if (lookingForEnd && (inLegend(tab[i][j]) || Character.isDigit(tab[i][j])))
					{
						nextStrings[i] = true;
						hasData = true;
					}
				}
			if (lookingForEnd)
			{
				boolean breakingPoint = true;
				for (int i = 0; i < 6; i++)
					if (usedStrings[i] && nextStrings[i])
					{
						breakingPoint = false;
						break;
					}
				if (breakingPoint)
				{
					NoteGroup notes = new NoteGroup(getPiece(tab, lowerBound, j));
					if (hasData)
						j--;
					newTab.add(notes);
					lookingForEnd = false;
					for (int i = 0; i < 6; i++)
						usedStrings[i] = true;
				}
				else
				{
					usedStrings = nextStrings;
					nextStrings = new boolean[6];
				}
			}
			hasData = false;
		}
		return newTab;
	}

	protected static boolean inLegend(char character)
	// Returns true if character is in legend
	{
		for (char modifier: legend)
			if (character == modifier)
				return true;
		return false;
	}

	private char[][] getPiece(char[][] array, int lowerBound, int upperBound)
	// Returns a chunk of 2D character array, given a lowerbound and upperbound
	// (of columns)
	{
		char[][] newArray = new char[6][upperBound - lowerBound];
		for (int j = lowerBound; j < upperBound; j++)
			for (int i = 0; i < 6; i++)
			{
				if (j < array[i].length)
					newArray[i][j - lowerBound] = array[i][j];
				else
					newArray[i][j - lowerBound] = '¶';//arbitrary character
			}
		return newArray;
	}

	/*
	 * OPTIMIZATION
	 * METHODS*******************************************************
	 */

	private int[] nextComb(int[] prev, int numGroupings)
	// Returns next combination given a previous combination and max number indicator
	// Based on http://compprog.files.wordpress.com/2007/10/comb1.c
	{
		if (prev.length == 0)
		{
			int[] comb = { 0 };
			return comb;
		}

		int k = prev.length;
		int n = numGroupings;
		int i = k - 1;

		prev[i]++;
		while ((i >= 0) && (prev[i] > n - k + i) && prev[0] <= n - k) // If exceeded limit in place each place has own limit
		{
			i--;
			prev[i]++;
		}

		if (prev[0] > n - k)
		{
			int[] comb = new int[k + 1];
			for (int j = 0; j < k + 1; j++)
				comb[j] = j;
			return comb;
		}

		for (i = i + 1; i < k; ++i)
			prev[i] = prev[i - 1] + 1;
		return prev;
	}

	private ArrayList<NoteGroup> optimize(ArrayList<NoteGroup> noteGroups) throws Exception, TimeoutException
	// Pre: Receives an ArrayList of NoteGroups
	// Post: Returns an ArrayList of NoteGroups with the Notes set to a decided final tab value
	{
		boolean[] bestGrouping = new boolean[0];
		int noteGroupsSize = noteGroups.size();
		int numCombinations = (int) Math.pow(2, noteGroups.size() - 1);
		int[] comb = new int[0];

		for (int i = 0; i < numCombinations; i++) // All possible number of chooses
		{
			boolean[] groupings = new boolean[noteGroupsSize - 1];
			for (int indice : comb)
				groupings[indice] = true;
			boolean goodGrouping = true;
			int lowerBound = 0;
			for (int j = 0; j < noteGroupsSize - 1; j++)
				if (groupings[j] == true)
				{
					ArrayList<NoteGroup> group = new ArrayList<NoteGroup>(noteGroups.subList(lowerBound, j + 1));
					lowerBound = j + 1;
					if (!computeGroup(group))
					{
						goodGrouping = false;
						break;
					}
				}

			ArrayList<NoteGroup> group = new ArrayList<NoteGroup>(noteGroups.subList(lowerBound, noteGroupsSize));
			if (goodGrouping)
				if (computeGroup(group)) // check the last group as well
				{
					bestGrouping = groupings;
					break;
				}
			comb = nextComb(comb, noteGroupsSize - 1);
		}
		ArrayList<NoteGroup> optimal = new ArrayList<NoteGroup>();
		if (bestGrouping.length == 0)
			throw new Exception("No possible fingering found.");

		int lowerBound = 0;
		for (int j = 0; j < noteGroups.size() - 1; j++)
		{
			if (bestGrouping[j] == true)
			{
				ArrayList<NoteGroup> group = new ArrayList<NoteGroup>(noteGroups.subList(lowerBound, j + 1));
				optimal.addAll(setToBest(group));
				lowerBound = j + 1;
			}
		}
		ArrayList<NoteGroup> group = new ArrayList<NoteGroup>(noteGroups.subList(lowerBound, noteGroupsSize));
		optimal.addAll(setToBest(group));
		//print(bestGrouping);

		return optimal;
	}

	private boolean computeGroup(ArrayList<NoteGroup> group) throws TimeoutException
	// To find that there exists a viable solution within bounds of the grouping
	{
		ArrayList<Note> allNotes = new ArrayList<Note>(); // instantiate all notes together															
		for (int i = 0; i < group.size(); i++)
		{
			NoteGroup thisGroup = group.get(i);
			ArrayList<Note> noteList = thisGroup.getNoteList();
			for (int j = 0; j < noteList.size(); j++)
			{
				Note tempNote = noteList.get(j);
				if (!tempNote.zero)// do not consider open notes
					allNotes.add(tempNote);
			}
		}

		if (allNotes.size() == 0)
			return true;

		//Perform setup for the permutation algorithm
		int[] x = new int[allNotes.size()];
		for (int i =0; i < x.length; i++)
			x[i] = allNotes.get(i).numPos;		
		int[] base = new int[x.length];
		base[base.length-1] = 1;
		for (int i = base.length-2; i >=0; i--)
			base[i] = x[i+1]*base[i+1];
		int permutations = x[0]*base[0];
		
		
		
		for (int i =0; i < permutations; i++) //We iterate through all the permutations of positioning fingers
		{
			int[] newNum = new int[base.length]; //Each permutation has an ID, which is mapped to an array
			//newNum array keeps track of the permutations
			//For example: suppose I have 3 notes, one with 3 testIndices, one with 1, and one with 2
			//Permutations will start with {0,0,0}, then {0,0,1}, then {1,0,0}, then {1,0,1}, etc..
			int ID = i;
			for (int j=0; j < base.length; j++)
			{
				newNum[j] = ID/base[j];
				ID = ID%base[j];
			}
			
						
			for (int j = 0; j < newNum.length; j++)
				allNotes.get(j).gotoIndice(newNum[j]);
			
			
			boolean thisWorks = true;
			int greatestFret = allNotes.get(0).positions[allNotes.get(0).testIndice];
			int leastFret = greatestFret;
			//System.out.println(leastFret);
			
			for (Note thisNote : allNotes) //test for max stretch violation in the group of NoteGroups
			{		
								
				int tempInt = thisNote.positions[thisNote.testIndice];
				if (tempInt < leastFret)
					leastFret = tempInt;
				if (tempInt > greatestFret)
					greatestFret = tempInt;
				if (greatestFret - leastFret > fretSpan)
				{
					thisWorks = false;
					break;
				}
			}
			
			//System.out.println("Greatest: " + greatestFret);
			//System.out.println("Least: " + leastFret);


			for (NoteGroup thisGroup: group) //test for max stretch violation within each NoteGroup
				if (!thisGroup.checkGroup())
					thisWorks = false;

			if (thisWorks)
				return true;
			
			if ((System.currentTimeMillis() - startTime)/1000>=10)//if timeout exceeds 10 seconds
				throw new TimeoutException();
		}
		
		//System.out.println("bad group");
		return false;
	}
	
	private ArrayList<NoteGroup> setToBest(ArrayList<NoteGroup> group)
	// To find that there exists a viable solution within bounds of the grouping
	{
		ArrayList<Note> allNotes = new ArrayList<Note>(); // instantiate all notes together															
		for (int i = 0; i < group.size(); i++)
		{
			NoteGroup thisGroup = group.get(i);
			ArrayList<Note> noteList = thisGroup.getNoteList();
			for (int j = 0; j < noteList.size(); j++)
			{
				Note tempNote = noteList.get(j);
				if (!tempNote.zero)// do not consider open notes
					allNotes.add(tempNote);
			}
		}

		if (allNotes.size() != 0)
		{

		int[] x = new int[allNotes.size()];
		for (int i =0; i < x.length; i++)
			x[i] = allNotes.get(i).numPos;		
		int[] base = new int[x.length];
		base[base.length-1] = 1;
		for (int i = base.length-2; i >=0; i--)
			base[i] = x[i+1]*base[i+1];
		int permutations = x[0]*base[0];
		
		
		for (int i =0; i < permutations; i++)
		{
			int[] newNum = new int[base.length];
			int ID = i;
			for (int j=0; j < base.length; j++)
			{
				newNum[j] = ID/base[j];
				ID = ID%base[j];
			}
			
			for (int j = 0; j < newNum.length; j++)
					allNotes.get(j).gotoIndice(newNum[j]);
			
			boolean thisWorks = true;
			int greatestFret = allNotes.get(0).positions[allNotes.get(0).testIndice];
			int leastFret = greatestFret;
			for (Note thisNote : allNotes) //test for max stretch violation in the group of NoteGroups
			{
				int tempInt = thisNote.positions[thisNote.testIndice];
				if (tempInt < leastFret)
					leastFret = tempInt;
				if (tempInt > greatestFret)
					greatestFret = tempInt;
				if (greatestFret - leastFret > fretSpan)
				{
					thisWorks = false;
					break;
				}
			}

			for (NoteGroup thisGroup: group) //test for max stretch violation within each NoteGroup
				if (!thisGroup.checkGroup())
					thisWorks = false;
			if (thisWorks)
				break;
		}
		}
		for (NoteGroup thisGroup : group)
			for (Note tempNote : thisGroup.getNoteList())
				tempNote.createDecision();
		
		return group;
	}
	
	/*
	 * TAB FINALIZATION
	 * *********************************************************
	 * ********************************
	 */

	private String finishedTab(ArrayList<NoteGroup> finishedNoteGroups)
	// Pre: Receives the completed NoteGroups, set with finalized definitions
	// Post: Returns the final tab string
	{	
		StringBuffer[] newTab = initializeTab();
		for (NoteGroup group: finishedNoteGroups)
		{
			ArrayList<Note> temp = group.getNoteList();
			int greatestLength = 0;
			int previousLength = newTab[0].length();
			for (Note tempNote: temp)
			{
				newTab[tempNote.testIndice].append(tempNote.decision);
				if (tempNote.decision.length() > greatestLength)
					greatestLength = tempNote.decision.length();
			}

			//"Round-off" groups so lines are all same length
			for (StringBuffer line: newTab)
				while (line.length() < previousLength + greatestLength)
					line.append("-");

			//Add some spacing between NoteGroups
			for (StringBuffer line: newTab)
				line.append("---");
		}
		
		//Add final touches and return as single string 
		StringBuffer finalizedTab = new StringBuffer();
		for (StringBuffer line: newTab)
			line.append("|");
		for (StringBuffer line: newTab)
			finalizedTab.append(line + "\n");
		return finalizedTab.toString();
	}

	private StringBuffer[] initializeTab()
	// Self-explanatory
	{
		StringBuffer[] newTab = new StringBuffer[6];
		newTab[0] = new StringBuffer("E|---");
		newTab[1] = new StringBuffer("A|---");
		newTab[2] = new StringBuffer("D|---");
		newTab[3] = new StringBuffer("G|---");
		newTab[4] = new StringBuffer("B|---");
		newTab[5] = new StringBuffer("e|---");
		return newTab;
	}

	/*
	 * DEBUGGING METHODS
	 * ********************************************************
	 * ***************************************
	 */

	

}
