import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;

public class JUnitTestCreator 
{
	private int[] actionIndecies;
	private HashMap<Integer, ArrayList<String>> javaCodePiecesMap;
	private HashMap<Integer, String> poolwideMap;
	private SUTInterface sut;
	private ArrayList<String> importLines;


	public JUnitTestCreator(int[] actionIds, SUTInterface sut) 
	{
		super();
		this.actionIndecies = actionIds;
		this.sut = sut;
	}

	private String[] genearateActionLines() 
	{
		String[] array = new String[actionIndecies.length];
		for (int x = 0; x < actionIndecies.length; x++) 
		{
			String line = "";
			Action action = sut.getActions()[actionIndecies[x]];
			int wasInit = 0;
			if(action.initId() != -1)
			{
				line += (makeLocalVariableName(action, 0) + " = ");
				wasInit = 1;
			}
			
			ArrayList<String> javaList = javaCodePiecesMap.get(new Integer(action.familyId()));
			for(int y = 0; y < javaList.size(); y++)
			{
				int varIndex = y + wasInit;
				line += javaList.get(y).trim();
				if(varIndex < action.repIds().length)
					line += makeLocalVariableName(action, varIndex);
				
			}
			array[x] = (line + ";");
		}
		return array;
	}

	private String[] generateLocalVariables() 
	{
		ArrayList<String> lines = new ArrayList<String>();
		if(poolwideMap == null)
			return null;
		ArrayList<Integer> varBeenInited = new ArrayList<Integer>();
		for (int x = 0; x < actionIndecies.length; x++) 
		{
			Action action = sut.getActions()[actionIndecies[x]];
			int initId = action.initId();
			if(initId != -1)
			{
				boolean already = false;
				for (int y = 0; y < varBeenInited.size(); y++) 
				{
					if(initId == varBeenInited.get(y))
					{
						already = true;
						break;
					}
				}
				if(!already)
				{
					String className = poolwideMap.get(new Integer(initId)).split(TstlConstants.SPLIT_SYNTAX_POOLVAL_CLASSNAME_WITH_VARNAME)[0];
					String initLine = className + " " + makeLocalVariableName(action,  0) + ";";
					lines.add(initLine);
					varBeenInited.add(initId);
				}
			}
		}
		return lines.toArray(new String[lines.size()]);
	}

	public int[] getActionIds() 
	{
		return actionIndecies;
	}


	private String makeLocalVariableName(Action action,  int varNum)
	{
		String gotFromMap = poolwideMap.get(action.repIds()[varNum]);
		String[] splitFromMap = gotFromMap.split(TstlConstants.SPLIT_SYNTAX_POOLVAL_CLASSNAME_WITH_VARNAME);
		String varName = splitFromMap[1].toLowerCase().trim();
		int num = action.repVals()[varNum];
		if(!(gotFromMap.startsWith(TstlConstants.POOLWIDEMAP_IDENTIFIER_NUMRANGE_CONSTANT)))
		{
			return varName + num;
		}
		else
		{
			int low = Integer.parseInt(varName);
			return (low + num) + "";
		}
	}

	private void parseJavaCodePiecesMap()
	{
		ArrayList<Integer> familyIdsList = new ArrayList<Integer>();
		for (int x = 0; x < actionIndecies.length; x++) 
		{
			int familyId = sut.getActions()[actionIndecies[x]].familyId();
			if(!familyIdsList.contains(new Integer(familyId)))
				familyIdsList.add(familyId);
		}
		int[] familyIdsArray = new int[familyIdsList.size()];
		for(int i = 0; i < familyIdsList.size(); i++)
		{
			familyIdsArray[i] = familyIdsList.get(i);
		}
		Arrays.sort(familyIdsArray);
		javaCodePiecesMap = new HashMap<Integer, ArrayList<String>>();
		BufferedReader reader = null;
		File javaCodePiecesMapFile = new File(TstlConstants.fileInDir(TstlConstants.getTstlHomeDir(), TstlConstants.FILE_JAVA_CODE_PIECE_SAVE));
		int putCount = 0;
		try
		{
			reader = new BufferedReader(new FileReader(javaCodePiecesMapFile));
			while(true)
			{
				String line = null;
				line = reader.readLine();
				if(line != null)
				{
					String[] mainSplit = (" "+line+" ").split(TstlConstants.SPLIT_SYNTAX_ID_WITH_CODE_PIECES);
					int id = Integer.parseInt(mainSplit[0].trim());
					if(id != familyIdsArray[putCount])
						continue;
					String codePiecesUnsplit = mainSplit[1];
					String[] codePiecesSplit = codePiecesUnsplit.split(TstlConstants.SPLIT_SYNTAX_JAVA_CODE_PIECES);
					ArrayList<String> codePieces = new ArrayList<String>();
					for (int i = 0; i < codePiecesSplit.length; i++) 
					{
						codePieces.add(codePiecesSplit[i]);
					}					
					javaCodePiecesMap.put(new Integer(id), codePieces);
					putCount++;
				}
				else
					break;
			}
		}
		catch(Exception ex)
		{
			TstlConstants.log(Level.SEVERE,"Failed to read java code pieces from file!",ex);
		}
		finally
		{
			if(reader != null)
			{
				try
				{
					reader.close();
				}
				catch(Exception ex)
				{
					TstlConstants.log(Level.WARNING, "Failed to close poolwidemap reader.",ex);
				}
			}
		}
	}

	private void parsePoolEntryMap() 
	{
		BufferedReader reader = null;
		File poowideMapFile = new File(TstlConstants.fileInDir(TstlConstants.getTstlHomeDir(), TstlConstants.FILE_POOLWIDE_MAP));
		poolwideMap = new HashMap<Integer,String>();
		try
		{
			reader = new BufferedReader(new FileReader(poowideMapFile));
			while(true)
			{
				String line = null;
				line = reader.readLine();
				if(line != null)
				{
					String[] split = line.split(TstlConstants.SPLIT_SYNTAX_POOLENTRY_ID_AND_CLASSNAME);
					int id = Integer.parseInt(split[0]);
					String className = split[1];
					poolwideMap.put(id,className);
					
				}
				else
					break;
			}
		}
		catch(Exception ex)
		{
			TstlConstants.log(Level.SEVERE,"Failed to read poolwidemap from file!",ex);
		}
		finally
		{
			if(reader != null)
			{
				try
				{
					reader.close();
				}
				catch(Exception ex)
				{
					TstlConstants.log(Level.WARNING, "Failed to close poolwidemap reader.",ex);
				}
			}
		}
	}
	
	private void parseImportLines() 
	{
		BufferedReader reader = null;
		File importLineFile = new File(TstlConstants.fileInDir(TstlConstants.getTstlHomeDir(), TstlConstants.FILE_IMPORTS_SAVE));
		importLines = new ArrayList<String>();
		try
		{
			reader = new BufferedReader(new FileReader(importLineFile));
			while(true)
			{
				String line = null;
				line = reader.readLine();
				if(line != null)
				{
					importLines.add(line);
				}
				else
					break;
			}
		}
		catch(Exception ex)
		{
			TstlConstants.log(Level.SEVERE,"Failed to read importlines from file!",ex);
		}
		finally
		{
			if(reader != null)
			{
				try
				{
					reader.close();
				}
				catch(Exception ex)
				{
					TstlConstants.log(Level.WARNING, "Failed to close importlines reader.",ex);
				}
			}
		}
	}

	public void setActionIds(int[] actionIds) 
	{
		this.actionIndecies = actionIds;
	}

	public void writeTest() 
	{
		parsePoolEntryMap();
		parseJavaCodePiecesMap();
		parseImportLines();
		String[] initLines = generateLocalVariables();
		String[] actionLines = genearateActionLines();
		File failureClassFile = new File(TstlConstants.fileInDir(TstlConstants.getPath(TstlConstants.PATHKEY_WORKINGDIR), TstlConstants.FILE_FAILING_TEST_OUTPUT_SOURCE));
		PrintWriter writer = null;
		try
		{
			failureClassFile.createNewFile();
			writer = new PrintWriter(failureClassFile);
		}
		catch(IOException ex)
		{
			TstlConstants.log(Level.WARNING, "Failed to create failing test source file.", ex);
		}
		if(writer == null)
			return;
		for (int i = 0; i < importLines.size(); i++) 
		{
			writer.println(importLines.get(i));
		}
		writer.println("public class " + "Failure");
		writer.println("{");
		writer.println("public static void main(String[] args0)");
		writer.println("{");
		for (int i = 0; i < initLines.length; i++) 
		{
			writer.println(initLines[i]);
		}
		writer.println();
		for (int i = 0; i < actionLines.length; i++) 
		{
			writer.println(actionLines[i]);
		}
		writer.println("}//main method closing");
		writer.println("}//class closing");
		writer.flush();
		writer.close();
	}
}
