package bettertab;

import java.util.ArrayList;

public class Debug
//Various debugging methods
{
	
	public static void print(String bla)
	{
		System.out.println(bla);
	}

	public static void print(int bla)
	{
		System.out.println(bla);
	}

	public static void print(ArrayList<Note> temp)
	{
		for (int i = 0; i < temp.size(); i++)
		{
			Note tempNote = (Note) temp.get(i);
			print(tempNote.decision);
		}

	}

	public static void print(int[] array)
	{
		for (int num : array)
			System.out.print(num + ", ");
		System.out.println();
	}

	public static void print(boolean[] array)
	{
		for (boolean bool : array)
			System.out.print(bool + ", ");
		System.out.println();
	}

	public static void print(long[] array)
	{
		for (long num : array)
			System.out.print(num + ", ");
		System.out.println();
	}

	public static void print(char[][] array)
	{
		for (int i = 0; i < array.length; i++)
		{
			for (int j = 0; j < array[i].length; j++)
				System.out.print(array[i][j] + ",");
			System.out.print("|");
		}
		System.out.println();
	}
	
	
	public static void printNote(int[] positions)
	//Prints out the different positions of a notes. Useful for debugging
	{
		for (int i =0; i < 6; i++)
			System.out.print(positions[i] + ", ");
		System.out.println();
	}
	
		
}
