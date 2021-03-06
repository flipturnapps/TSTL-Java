
public class NumRange extends Repeatable 
{
	private int low;
	private int hi;

	public static NumRange getNumRange(String s)
	{
		String[] pieces = s.replace(TstlConstants.IDENTIFIER_NUMRANGE_LEFT, "").replace(TstlConstants.IDENTIFIER_NUMRANGE_RIGHT, "").replace(TstlConstants.IDENTIFIER_NUMRANGE_MID,"~").split("~");
		if(pieces.length < 2 || pieces.length > 2)
			return null;
		int low = -1;
		int hi = -1;
		try
		{
			low = Integer.parseInt(pieces[0]);
			hi = Integer.parseInt(pieces[1]);
		}
		catch(RuntimeException ex)
		{
			return null;
		}
		return new NumRange(low,hi);
	}
	private NumRange(int low, int hi) 
	{
		super();
		this.low = low;
		this.hi = hi;
		poolwideMap.put(this.getId(), TstlConstants.POOLWIDEMAP_IDENTIFIER_NUMRANGE_CONSTANT + TstlConstants.SPLIT_SYNTAX_POOLVAL_CLASSNAME_WITH_VARNAME + low);
	}

	@Override
	public int getListSize() 
	{
		return hi-low+1;
	}

	@Override
	public String getAsJava(int i)
	{
		return (low+i)+"";
	}
	@Override
	public String getIsUsableExpression(int i) 
	{
		return true+"";
	}
	@Override
	public String getCanOverwriteExpression(int i)
	{
		return true+"";
	}
	@Override
	public boolean equalsRepeatable(Repeatable rep)
	{
		if(!(rep instanceof NumRange))
			return false;
		NumRange range = (NumRange) rep;
		if(range.low == this.low && range.hi == this.hi)
			return true;
		return false;
	}
	@Override
	public String toString() 
	{
		return "NumRange [low=" + low + "- hi=" + hi + "]";
	}
	public String getAsTstl()
	{
		String ret = "";
		ret += TstlConstants.IDENTIFIER_TSTLVARIABLE;
		ret += TstlConstants.IDENTIFIER_NUMRANGE_LEFT;
		ret += this.low;
		ret += TstlConstants.IDENTIFIER_NUMRANGE_MID;
		ret += this.hi;
		ret += TstlConstants.IDENTIFIER_NUMRANGE_RIGHT;
		ret += TstlConstants.IDENTIFIER_TSTLVARIABLE;
		return ret;
	}
	@Override
	public String getAsFormattedTstl(int i) 
	{
		return getAsJava(i);
	}
}




