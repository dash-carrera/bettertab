package bettertab;


import java.text.ParseException;
import java.util.ArrayList;

//This is a "note group." It is all the notes that occur AT THE SAME TIME, accounting for special characters.
//Most of the methods originally planned for this class ended up being placed in optimal_finder, so it is a relatively short class.

class NoteGroup {

	ArrayList<Note> noteList = new ArrayList<Note>();
	
	
	protected NoteGroup(char[][] notes) throws ParseException
	//This method receives a chunk of tab with notes that occur at the same time, turns them into notes, and puts them in a list together
	{
		for (int i =0; i < 6; i++)
		{
			boolean empty = true;	
			ArrayList<Character> temp1 = new ArrayList<Character>();
			for (int j = 0; j < notes[0].length; j++)
				if (optimal_finder.inLegend(notes[i][j]) || Character.isDigit(notes[i][j]))
				{
					temp1.add(new Character(notes[i][j]));
					empty = false;
				}
			if (!empty)
			{
			char[] temp2 = new char[temp1.size()];
			for (int j =0; j < temp2.length; j++)
				temp2[j] = temp1.get(j).charValue();
			Note note = new Note(temp2,i);
			noteList.add(note);
			}
		}
		zeroCheck();
	}
	
	
	NoteGroup(ArrayList<Note> notes)
	{
		noteList = notes;
	}
	
	protected void zeroCheck()
	//We use open notes when available, unless the result is unplayable tab
	{
		zeroCheck:
		for (Note note: noteList)
		{
			int open_string = note.zeroCheck(); //Get string open note can be played on, -1 otherwise
			if (open_string != -1 && !note.special)
			{
				for (Note other_note: noteList)//Check all the other notes
					if (!other_note.equals(note))
					{
						boolean options = false;
						for (int i = 0; i < 6; i++)
							if (other_note.positions[i]!=-1 && i!=open_string)//If other_note has option
							{
								options = true;
								break;
							}
						if (!options)
							continue zeroCheck; //Other notes presented with no better option
					}
				note.zeroSet(); //Other notes have options - commit note
				for (Note other_note: noteList)//Commit other notes to not use option
					if (!other_note.equals(note))
					{
						other_note.positions[open_string] = -1;
						other_note.numPos--;
					}
			}
		}
		
	}
	
	
	protected ArrayList<Note> getNoteList()
	//Returns all the Notes as an ArrayList
	{
		return noteList;
	}
	
	
	protected boolean checkGroup()
	//Check that none of the tested positions for notes in the group are on the same string
	{	
		boolean[] usedTestIndices = new boolean[6];
		for (Note note: noteList)
		{
			if (usedTestIndices[note.testIndice] == false)
				usedTestIndices[note.testIndice] = true;
			else 
				return false;
		}
		return true;
	}
}