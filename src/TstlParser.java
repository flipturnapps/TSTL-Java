import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.logging.Level;


public class TstlParser implements Runnable
{
	private ArrayList<String> tstl;
	private FlushWriter writer;
	private PoolEntry[] poolEntries;
	private int countActionsPrinted;
	private PropertyEntry[] propEntries;
	
	public static void main(String[] args) throws IOException
	{
		TstlParserArgParser parser = new TstlParserArgParser(args);
		parser.parse();
		String line = "";
		for (int i = 0; i < args.length; i++) 
		{
			if(i != 0)
				line += " ";
			line += args[i];			
		}
		File argsFile = new File(TstlConstants.fileInDir(TstlConstants.getTstlHomeDir(),(TstlConstants.FILENAME_ARGSSTORE)));
		argsFile.createNewFile();
		PrintWriter writer = new PrintWriter(argsFile);
		writer.println(line);
		writer.flush();
		writer.close();

		if(parser.shouldSkipParse())
		{
			TstlConstants.writeHomeFile(TstlConstants.FILE_WASHELP, false +"");
			TstlConstants.writeHomeFile(TstlConstants.FILE_SKIPCOMPILE, true +"");
			System.exit(0);
		}
		TstlConstants.writeHomeFile(TstlConstants.FILE_SKIPCOMPILE, false +"");
		TstlConstants.writeHomeFile(TstlConstants.FILE_WASHELP, true +"");
		new Thread(new TstlParser()).start();
	}
	@Override
	public void run()
	{
		TstlConstants.outputDependencies();

		readTstl();

		createOutWriter();

		readImports();

		readCCSource();
		
		//Makes Class Declaration using the filename from the input
		generateClassDeclaration();

		generatePoolEntries();
		
		generatePropertyEntries();

		generateInstanceVariables();
		
		generateCheckMethod();
		
		generateSetReInitValueMethod();

		generateConstructor();

		generateClearPool();

		generateActionsInit();

		generateGetActions();

		generateReset();

		finishingTouches();

		compileGeneratedClasses();
	}

	
	private void generateSetReInitValueMethod()
	{
		writer.println("private int " + TstlConstants.DECLARATION_SUT_ALLOW_REINIT_VALUE_FIELD + " = "+TstlConstants.DEFUALT_SUT_REINIT_VALUE+";");
		writer.println("public void " + TstlConstants.DECLARATION_SUT_SET_RENIT_VALUE_METHOD + "(int val) {");
		writer.println(TstlConstants.DECLARATION_SUT_ALLOW_REINIT_VALUE_FIELD + " = val;");
		writer.println("}");
	}
	private void readCCSource ()
	{
		TstlConstants.log(Level.INFO,"Starting readCCSource");
		ArrayList<String> sources = new ArrayList<String>();
		for (int i = 0; i < tstl.size(); i++)
		{
			String line = tstl.get(i);
			TstlConstants.log(Level.INFO,"i:" + i + " line:\"" + line + "\"");
			if(line.startsWith(TstlConstants.IDENTIFIER_CC_SOURCE))
			{
				String sourceInstruction = line.substring(TstlConstants.IDENTIFIER_CC_SOURCE.length()).trim();
				TstlConstants.log(Level.INFO,"sourceinstruction " + sourceInstruction);
				sources.add(sourceInstruction);
				tstl.remove(i); //newly added. remove if fails.
				TstlConstants.log(Level.INFO,"true, removed");				
				i--;
			}
		}
		TstlConstants.writeHomeFile(TstlConstants.FILE_WANTS_CC, (!(sources.isEmpty())) + "");
		if(!sources.isEmpty())
		{
			String line = "";
			for(int i = 0; i < sources.size(); i++)
			{
				line += "+" + sources.get(i) + ",";
				TstlConstants.log(Level.INFO,"heres the cccfg line: " + line);
			}		
			TstlConstants.log(Level.INFO,"heres the cccfg line: " + line);
			line = line.substring(0, line.length() -1);
			TstlConstants.log(Level.INFO,"heres the cccfg line, again: " + line);
			TstlConstants.writeHomeFile(TstlConstants.FILE_CC_CFG, line);
		}		
	}
	private void readTstl()
	{
		String filePath = TstlConstants.getPath(TstlConstants.PATHKEY_TSTLFILE);
		File file = new File(filePath);
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		ArrayList<String> lines = new ArrayList<String>();
		boolean hadNull = false;
		while(true)
		{
			String line = null;
			try {
				line = reader.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if(line == null)
			{
				if(!hadNull)
				{
					hadNull = true;
					continue;
				}
				else
					break;
			}
			TstlConstants.log("line: \"" + line + "\"");
			lines.add(line.replace(";", ""));
		}
		tstl = new ArrayList<String>();
		for(int i = 0; i < lines.size(); i++)
		{
			tstl.add(lines.get(i));
		}
	}
	private void createOutWriter() 
	{
		String outPath = getOutputFileFilepath();
		try {
			writer = new FlushWriter(new File(outPath));
		} catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		writer.println(TstlConstants.COMMENT_AUTO_GEN_CODE);
	}
	private void readImports() 
	{
		File importFile = new File(TstlConstants.fileInDir(TstlConstants.getTstlHomeDir(), TstlConstants.FILE_IMPORTS_SAVE));			
		PrintWriter importWriter = null;
		try 
		{
			importFile.createNewFile();
			importWriter = new PrintWriter(importFile);
		} 
		catch (IOException e) 
		{
			TstlConstants.log(Level.SEVERE, "Failed to write import file!", e);
			return;
		}
		finally
		{
			if(importWriter == null)
				return;
		}		
		for (int i = 0; i < tstl.size(); i++)
		{
			String line = tstl.get(i);
			if(line.startsWith(TstlConstants.IDENTIFIER_IMPORT))
			{
				String importObject = line.substring(TstlConstants.IDENTIFIER_IMPORT.length());
				if(!(importObject.trim().equals(TstlConstants.IMPORT_ARRAY_LIST) || importObject.trim().equals(TstlConstants.IMPORT_LIST)))
				{
					String importLine = "import " + importObject + ";"+TstlConstants.COMMENT_TSTL_SOURCE_IMPORT;
					importWriter.println(importLine);
					writer.println(importLine);
				}
				tstl.remove(i);
				i--;
			}
		}
		writer.println("import " + TstlConstants.IMPORT_LIST + ";"+TstlConstants.COMMENT_CORE_FUNCTIONALITY_IMPORT);
		writer.println("import " + TstlConstants.IMPORT_ARRAY_LIST + ";"+TstlConstants.COMMENT_CORE_FUNCTIONALITY_IMPORT);
		importWriter.flush();
		importWriter.close();
	}
	private void generateClassDeclaration()
	{
		String className = TstlConstants.CLASS_NAME_SUT;
		writer.println("public class " + className + " implements SUTInterface");
		writer.println("{");
	}
	private void generatePoolEntries()
	{
		ArrayList<PoolEntry> poolEntries = new ArrayList<PoolEntry>();
		for (int x = 0; x < tstl.size(); x++)
		{
			String line = tstl.get(x);
			if(line.startsWith(TstlConstants.IDENTIFIER_POOL))
			{
				String restLine = line.substring(TstlConstants.IDENTIFIER_POOL.length());
				String[] restLineSplit = restLine.split(" ");
				String className = null;
				String varName = null;
				int arrSize = 1;
				for (int y = 0; y < restLineSplit.length; y++) 
				{
					String piece = restLineSplit[y];
					boolean hasParentheses = piece.contains(TstlConstants.IDENTIFIER_TSTLVARIABLE);
					boolean canParseInt = false;
					int parsed = -1;
					try
					{
						parsed = Integer.parseInt(piece);
						canParseInt = true;
					}
					catch(RuntimeException ex){}

					if(canParseInt)
						arrSize = parsed;					
					else if(hasParentheses)
					{
						varName = removePercents(piece);
					}
					else
					{
						if(piece != null)
							className = piece;
					}
				}	
				if(className == null || varName == null || arrSize < 1)
					throw new MalformedTstlException(TstlConstants.MESSAGE_MALFORMED_POOL_DECLARATION + "\"" + line + "\"");
				PoolEntry entry = new PoolEntry(className, varName, arrSize);
				poolEntries.add(entry);
				tstl.remove(x);
				x--;
			}
		}

		this.poolEntries = new PoolEntry[poolEntries.size()];
		for (int i = 0; i < this.poolEntries.length; i++) 
		{
			this.poolEntries[i] = poolEntries.get(i);
		}
	}
	
	private void generatePropertyEntries()
	{		
		ArrayList<PropertyEntry> arrListPropEntries = new ArrayList<PropertyEntry>();
		for (int x = 0; x < tstl.size(); x++)
		{
			String line = tstl.get(x);
			if(line.startsWith(TstlConstants.IDENTIFIER_PROPERTY))
			{
				String restLine = line.substring(TstlConstants.IDENTIFIER_PROPERTY.length());
				LineParsePacket packet = TstlConstants.parseVarLine(restLine, poolEntries);				
				PropertyEntry propEntry = new PropertyEntry(packet);
				arrListPropEntries.add(propEntry);			
				tstl.remove(x);
				x--;
			}
		}
		propEntries = arrListPropEntries.toArray(new PropertyEntry[arrListPropEntries.size()]);
	}
	
	private void generateInstanceVariables()
	{
		for(int i =0; i< this.poolEntries.length; i++)
		{
			PoolEntry entry = poolEntries[i];
			writer.println(entry.getInstanceVariableDeclaration(TstlConstants.VISIBILITY_LEVEL_POOL_VAR));
		}
		writer.println(TstlConstants.DECLARATION_ACTION_ARRAY_INSTANCE_VARIABLE);
	}
	
	private void generateConstructor()
	{
		writer.println("public " + TstlConstants.CLASS_NAME_SUT + "()"+ "\n"
				+"{"+ "\n"
				+"clearPool();"+ "\n"
				+"actionsInit();"+ "\n"
				+"}"+ "\n");

	}
	private void generateClearPool() 
	{
		writer.println(TstlConstants.DECLARATION_CLEAR_POOL_METHOD);
		for (int i = 0; i < poolEntries.length; i++) 
		{
			PoolEntry entry = poolEntries[i];
			writer.println(entry.getClearLines());
		}
		writer.println("}");

	}
	private void generateCheckMethod() 
	{
		writer.println(PropertyEntry.generateCheck(propEntries));		
	}
	private void generateActionsInit() 
	{
		File javaCodePieceFile = new File(TstlConstants.fileInDir(TstlConstants.getTstlHomeDir(), TstlConstants.FILE_JAVA_CODE_PIECE_SAVE));
		PrintWriter javaCodePieceWriter = null;
		try 
		{
			javaCodePieceFile.createNewFile();
			javaCodePieceWriter = new PrintWriter(javaCodePieceFile);
		} catch (IOException e) 
		{
			TstlConstants.log(Level.SEVERE,"Failed to write to javaCodePieceFile.",e);
		}
		writer.println(TstlConstants.DECLARATION_ACTIONS_INIT_METHOD + " {");
		ArrayList<String> actionLines = new ArrayList<String>();
		int totalCount = 0;
		//can be omitted in the future for more efficent code
		for(int i = 0; i < tstl.size(); i++)
		{
			if(!tstl.get(i).equals(""))
			{
				ActionEntry entry = makeActionEntry(tstl.get(i),-1);
				actionLines.add(tstl.get(i));
				totalCount += entry.getActionCount();
			}
		}	
		writer.println(TstlConstants.CONSTRUCT_ACTION_ARRAY_INSTANCE_VARIABLE + totalCount + "];");
		writer.println(TstlConstants.DECLARATION_ACTION_LOCAL_VARIABLE);
		for(int i = 0; i < actionLines.size(); i++)
		{			
			ActionEntry entry = this.makeActionEntry(actionLines.get(i),i);
			RepeatablesAction action = new RepeatablesAction()
			{
				@Override
				public void actOnRepValues(int[] vals, RepeatablesContainer cont)
				{
					ActionEntry aEntry = (ActionEntry) cont;
					writer.println(aEntry.createActionClass(vals));		
					writer.println("actions[" + countActionsPrinted + "] = action;");
					countActionsPrinted++;		
				}

			};
			entry.actOnValidCombinations(action);
			if(javaCodePieceWriter != null)
				javaCodePieceWriter.println(entry.getJavaCodePiecesAsSaveLine());
		}
		if(javaCodePieceFile != null)
		{
			javaCodePieceWriter.flush();
			javaCodePieceWriter.close();
		}
		writer.println("}//close actionInit()");
	}
	private ActionEntry makeActionEntry(String tstlLine, int familyId) 
	{
		String[] parts = ActionEntry.splitActionLine(tstlLine);
		ActionEntry entry = new ActionEntry(parts[0],parts[1],this.poolEntries,familyId);
		return entry;
	}
	

	private void generateGetActions() 
	{
		String ret = TstlConstants.DECLARATION_SUT_GETACTIONS_METHOD + "\n";
		ret +="{\n";
		ret +="return actions;\n";
		ret +="}//close reset()\n";
		writer.print(ret);
	}

	private void generateReset()
	{
		String ret = TstlConstants.DECLARATION_SUT_RESET_METHOD + "\n";
		ret +="{\n";
		ret +="clearPool();\n";
		ret +="}//close reset()\n";
		writer.print(ret);
	}

	private void finishingTouches()
	{
		writer.println("}//class close brace"); 
		writer.close();
		TstlConstants.closeLogger();
		TstlConstants.writeHomeFile(TstlConstants.FILE_WASHELP, false +"");
		Repeatable.writePoolwideMapToFile();
		System.out.println("finished");	
	}	

	private void compileGeneratedClasses() 
	{
		TstlConstants.getGeneratedClassesFolder();
	}

	private String getOutputFileFilepath() 
	{
		return TstlConstants.getParserOutputSourceDir()+ TstlConstants.CLASS_NAME_SUT+".java";
	}	

	private String removePercents(String line, String variablePrefix)
	{
		//used to replace TSTL variables in a line with ones that would compile in java]
		//ie "int %INT% = 5" becomes "int var_INT = 5"
		line = " " + line + " ";

		String[] percentBlocks = line.split(TstlConstants.IDENTIFIER_TSTLVARIABLE);
		if(percentBlocks.length % 2 == 0)
			throw new MalformedTstlException(TstlConstants.MESSGAGE_NONSURROUNDING_VARIABLE_IDENTIFIERS + line);
		for(int i = 1; i < percentBlocks.length; i+= 2)
		{
			String block = percentBlocks[i];

			percentBlocks[i] = variablePrefix + block;
		}
		String newLine = "";
		for(int i = 0; i < percentBlocks.length; i++)
		{
			newLine += percentBlocks[i];
		}
		return newLine;
	}
	private String removePercents(String line)
	{
		return removePercents(line,TstlConstants.PREFIX_JAVA_VARIABLES);
	}

}
