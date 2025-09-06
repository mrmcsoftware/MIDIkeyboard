/* Copyright © 2017 Mark Craig (https://www.youtube.com/MrMcSoftware) */

package com.mcsoftware.logisim.mykeyboardlib;

import java.util.Date;
import java.util.Calendar;
import java.util.TimeZone;
import java.net.URL;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import javax.sound.sampled.LineEvent.Type;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.lang.reflect.*;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstancePoker;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.util.StringUtil;
import com.cburch.logisim.util.StringGetter;
import com.cburch.logisim.util.GraphicsUtil;

public class Keyboard extends InstanceFactory
{
	private static final int DELAY = 1; // really don't know what this should be
	private static Method mGetPort,mToValue,mCreateKnown,mGetBounds;
	private Object mArgs[] = new Object[1], mArgs2[] = new Object[2];
	private static int noteVal = -1, noteX = -1, noteY, noteW, noteH;
	private static int colorNotes = 1;
	private boolean longvalues = false, longvalues2 = false;
	private static final int[] sharps = {1,1,0,1,1,1,0};
	private static int[] channels = {0,0,0,0,0,0,0,0};
	private static final String[] noteNames = {"C","D","E","F","G","A","B"};
	private static Color noteColor;
	public static final Attribute<Integer> ATTR_OCTAVEOFF = Attributes
		.forIntegerRange("octaveoff", new MyStringGetter("Octave Offset"), 0, 10);
	public static final Attribute<Integer> ATTR_OCTAVES = Attributes
		.forIntegerRange("octaverange", new MyStringGetter("Octave Range"), 1, 11);
	public static final Attribute<Integer> ATTR_CHANSEL = Attributes
		.forIntegerRange("channelsel", new MyStringGetter("Channel Selector"),0,8);
	public static final Attribute<Boolean> ATTR_COLORC = Attributes.forBoolean(
		"colorcode", new MyStringGetter("Color Code Keys"));
	public static final Attribute<Boolean> ATTR_LABELN = Attributes.forBoolean(
		"labelnotes", new MyStringGetter("Label Notes"));
	public static final Attribute<Boolean> ATTR_EXTRAC = Attributes.forBoolean(
		"extrac", new MyStringGetter("Extra C/C#"));
	public static final Attribute<Integer> ATTR_CHANNEL = Attributes
		.forIntegerRange("channel", new MyStringGetter("Channel"), 0, 7);

	// note frequencies used for the buzzer
	private static final double[] freqs = {
		8.176,8.662,9.177,9.723,10.301,10.913,11.562,12.25,12.978,13.75,14.568,15.434,
		16.4,17.3,18.4,19.4,20.6,21.8,23.1,24.5,26.0,27.5,29.1,30.9,
		32.7,34.6,36.7,38.9,41.2,43.7,46.2,49.0,51.9,55.0,58.3,61.7,
		65.4,69.3,73.4,77.8,82.4,87.3,92.5,98.0,103.8,110.0,116.5,123.5,
		130.8,138.6,146.8,155.6,164.8,174.6,185.0,196.0,207.7,220.0,233.1,246.9,
		261.6,277.2,293.7,311.1,329.6,349.2,370.0,392.0,415.3,440.0,466.2,493.9,
		523.3,554.4,587.3,622.3,659.3,698.5,740.0,784.0,830.6,880.0,932.3,987.8,
		1046.5,1108.7,1174.7,1244.5,1318.5,1396.9,1480.0,1568.0,1661.2,1760.0,1864.7,1975.5,
		2093.0,2217.5,2349.3,2489.0,2637.0,2793.8,2960.0,3136.0,3322.4,3520.0,3729.3,3951.1,
		4186.0,4434.9,4698.6,4978.0,5274.0,5587.7,5919.9,6271.9,6644.9,7040.0,7458.6,7902.1,
		8372.0,8869.8,9397.3,9956.1,10548.1,11175.3,11839.8,12543.9};

	private static class Channel implements InstanceData, Cloneable
	{
		/* I basically just created an InstanceDataSingleton */
		/* (reinvented the wheel) */

		private int chan;

		public Channel(int value)
		{
			chan = value;
		}

		public int get()
		{
			return chan;
		}

		@Override
		public Object clone()
		{
			try { return super.clone(); }
			catch (CloneNotSupportedException e) { return null; }
		}
	}

	public Keyboard()
	{
		super("MCKeyboard", new MyStringGetter("     Musical Keyboard"));
		setAttributes(new Attribute[] { ATTR_OCTAVEOFF, ATTR_OCTAVES, ATTR_CHANNEL, ATTR_CHANSEL, ATTR_COLORC, ATTR_LABELN, ATTR_EXTRAC },
			new Object[] { 1, 5, 0, 0, true, true, true });
		setFacingAttribute(StdAttr.FACING);
		/* URL url = getClass().getClassLoader().getResource("resources/logisim/icons/mycircuit.gif");
		setIcon(new ImageIcon(url)); */
		//setOffsetBounds(Bounds.create(0, 0, 720, 130));
		setInstancePoker(Poker.class);
		// Use Java's reflection to determine which version of Logisim
		Class c, params[] = new Class[1], params2[] = new Class[2];
		try
			{
			c = Class.forName("com.cburch.logisim.data.Value");
			params2[0] = BitWidth.class;
			params2[1] = Integer.TYPE;
			mCreateKnown = c.getMethod("createKnown", params2);
			}
		catch (Throwable e)
			{
			try
				{
				c = Class.forName("com.cburch.logisim.data.Value");
				params2[1] = Long.TYPE;
				mCreateKnown = c.getMethod("createKnown", params2);
				longvalues2 = true;
				}
			catch (Throwable e3) { System.out.println(e3); }
			}
		try
			{
			c = Class.forName("com.cburch.logisim.data.Value");
			params[0] = Integer.TYPE;
			mToValue = c.getMethod("toIntValue", null);
			/* Method m[] = c.getDeclaredMethods();
			for (int i=0; i<m.length; i++) { System.out.println(m[i].toString()); } */
			/* Is it original Logisim? */
			c = Class.forName("com.cburch.logisim.instance.InstanceState");
			mGetPort = c.getMethod("getPort", params);
			}
		catch (Throwable e)
			{
			/* probably Logisim Evolution */
			try
				{
				c = Class.forName("com.cburch.logisim.instance.InstanceState");
				mGetPort = c.getMethod("getPortValue", params);
				}
			catch (Throwable e2) { System.err.println(e2); }
			try
				{
				c = Class.forName("com.cburch.logisim.data.Value");
				mToValue = c.getMethod("toIntValue", null);
				}
			catch (Throwable e2)
				{
				/* newer versions of Evolutions that changed .toIntValue to
					.toLongValue */
				try
					{
					c = Class.forName("com.cburch.logisim.data.Value");
					mToValue = c.getMethod("toLongValue", null);
					longvalues = true;
					}
				catch (Throwable e3) { System.out.println(e3); }
				}
			}
		try
			{
			/* Is it original Logisim? */
			c = Class.forName("com.cburch.logisim.instance.InstancePainter");
			mGetBounds = c.getMethod("getBounds", null);
			}
		catch (Throwable e4)
			{
			/* newer versions of Holy Cross Evolution that changed .getBounds to
				.getNominalBounds */
			try
				{
				c = Class.forName("com.cburch.logisim.instance.InstancePainter");
				mGetBounds = c.getMethod("getNominalBounds", null);
				}
			catch (Throwable e5) { System.out.println(e5); }
			}
	}
	
	public void paintInstance(InstancePainter painter)
	{
		Graphics g = painter.getGraphics();
		//Bounds bds = painter.getBounds();
		try { Bounds bds = (Bounds)(mGetBounds.invoke(painter,null)); }
		catch (Throwable e6) { System.out.println(e6); }
		int x = painter.getLocation().getX();
		int y = painter.getLocation().getY();
		int i, dx, numKey, chan;
		int chanSel = painter.getAttributeValue(ATTR_CHANSEL);
		if (chanSel > 0)
			{
			Channel chanNum = (Channel)painter.getData();
			if (chanNum == null)
				{
				int val = painter.getAttributeValue(ATTR_CHANNEL);
				chanNum = new Channel(val);
				painter.setData(chanNum);
				}
			chan = chanNum.chan;
			}
		else { chan = painter.getAttributeValue(ATTR_CHANNEL); }
		GraphicsUtil.switchToWidth(g, 2);
		int oct = painter.getAttributeValue(ATTR_OCTAVES);
		int extraC = (painter.getAttributeValue(ATTR_EXTRAC)) ? 1 : 0 ;
		numKey = (7*oct+extraC); dx = numKey*20;
		if (chanSel > 0)
			{
			int dx2 = dx/2-((chanSel*15)/2);
			g.setColor(Color.BLACK);
			g.fillRect(x+dx2, y-16, chanSel*15, 15);
			for (i=0; i<chanSel; i++)
				{
				if (i == chan) { g.setColor(Color.YELLOW); }
				else { g.setColor(Color.RED); }
				g.fillRect(x+i*15+3+dx2, y-12, 9, 9);
				}
			g.setColor(Color.BLACK);
			}
		g.drawRect(x, y, dx, 130);
		for (i=1; i<numKey; i++)
			{
			g.drawLine(x+i*20, y, x+i*20, y+130);
			}
		if ((noteX > -1) && (noteH == 128))
			{
			g.setColor(noteColor);
			//g.setColor(Color.RED);
			g.fillRect(noteX, noteY, noteW, noteH);
			}
		g.setColor(Color.BLACK);
		for (i=1; i<numKey; i++)
			{
			if (sharps[(i-1)%7] == 1)
				{
				g.fillRect(x+i*20-6, y, 12, 60);
				}
			}
		if ((noteX > -1) && (noteH < 120))
			{
			g.setColor(noteColor);
			//g.setColor(Color.RED);
			g.fillRect(noteX, noteY, noteW, noteH);
			g.setColor(Color.BLACK);
			}
		GraphicsUtil.switchToWidth(g, 1);
		if (painter.getAttributeValue(ATTR_LABELN))
			{
			int octaveOff = painter.getAttributeValue(ATTR_OCTAVEOFF);
			for (i=0; i<numKey; i++)
				{
				String ns = noteNames[i%7]+(i/7+octaveOff);
				GraphicsUtil.drawCenteredText(g, ns, x+i*20+10, y+136);
				if (sharps[i%7] == 1)
					{
					ns = noteNames[i%7]+"#";
					GraphicsUtil.drawCenteredText(g, ns, x+i*20+20, y+146);
					}
				}
			}
		painter.drawPorts();
	}
	
	@Override
	public void paintIcon(InstancePainter painter)
	{
		Graphics g = painter.getGraphics();
		g.setColor(Color.BLACK);
		g.drawRoundRect(0, 2, 34, 14, 3, 2);
		g.setColor(Color.LIGHT_GRAY);
		g.fillRect(3, 4, 29, 11);
		g.setColor(Color.BLACK);
		g.fillRect(6, 4, 3, 5);
		g.fillRect(12, 4, 3, 5);
		g.fillRect(24, 4, 3, 5);
		g.fillRect(30, 4, 2, 5);
		g.drawRect(3, 4, 4, 10);
		g.drawRect(7, 4, 6, 10);
		g.drawRect(13, 4, 6, 10);
		g.drawRect(19, 4, 6, 10);
		g.drawRect(25, 4, 6, 10);
	}

	@Override
	public Bounds getOffsetBounds(AttributeSet attrs)
	{
		int extraC = (attrs.getValue(ATTR_EXTRAC)) ? 1 : 0 ;
		int chanSel = attrs.getValue(ATTR_CHANSEL);
		int yPos = (chanSel>0) ? -15 : 0 ;
		return Bounds.create(0, yPos, (7*attrs.getValue(ATTR_OCTAVES).intValue()+extraC)*20, 130-yPos);
	}

	@Override
	protected void configureNewInstance(Instance instance)
	{
		instance.addAttributeListener();
		updatePorts(instance);
	}
	
	@Override
	protected void instanceAttributeChanged(Instance instance, Attribute<?> attr)
	{
		if ((attr == ATTR_OCTAVES) || (attr == ATTR_EXTRAC) || (attr == ATTR_CHANSEL))
			{
			instance.recomputeBounds();
			}
		else { instance.fireInvalidated(); }
	}

	@Override
	public void propagate(InstanceState state)
	{
	Value mVal;
	int chan = -1;
	int chanSel = state.getAttributeValue(ATTR_CHANSEL);
	mArgs[0] = new Integer(10);
	try
		{
		/* Object ret = mGetPort.invoke(state, mArgs);
		Value v = (Value)(ret); */
		/* Value v = (Value)(mGetPort.invoke(state, mArgs));
		chan = v.toIntValue(); */
		/* equivalent to val = state.getPort(10).toIntValue(); */
		Value v = (Value)(mGetPort.invoke(state, mArgs));
		if (longvalues) { chan = (int)((long)(mToValue.invoke(v,null))); }
		else { chan = (int)(mToValue.invoke(v,null)); }
		//chan = ((Value)(mGetPort.invoke(state, mArgs))).toIntValue();
		//chan = (int)(((Value)(mGetPort.invoke(state, mArgs))).toLongValue());
		if ((chanSel > 0) && (chan >= 0)) { Channel chanNum = new Channel(chan); state.setData(chanNum); }
		if (chan < 0)
			{
			if (chanSel > 0)
				{
				Channel chanNum = (Channel)state.getData();
				if (chanNum == null)
					{
					int val = state.getAttributeValue(ATTR_CHANNEL);
					chanNum = new Channel(val);
					state.setData(chanNum);
					}
				chan = chanNum.chan;
				}
			else { chan = state.getAttributeValue(ATTR_CHANNEL); }
			}
		int note = state.getAttributeValue(ATTR_OCTAVEOFF)*12;
		if (noteVal >= 0) { note += noteVal; } else { note = 128; }
		if (note > 127) { note = 128; }
		channels[chan] = note;
		boolean colorCode = state.getAttributeValue(ATTR_COLORC);
		if (colorCode == true) { colorNotes = 1; } else { colorNotes = 0; }
		mArgs2[0] = BitWidth.create(8);
		for (int i=0; i<8; i++)
			{
			if (longvalues2) { mArgs2[1] = new Long(channels[i]); }
			else { mArgs2[1] = new Integer(channels[i]); }
			mVal=(Value)(mCreateKnown.invoke(null, mArgs2));
			state.setPort(i, mVal, DELAY);
			//state.setPort(i, Value.createKnown(BitWidth.create(8), channels[i]), DELAY);
			}
		//System.out.println(freqs.length);
		if (note < 128)
			{
			int fNote = (int)(freqs[note]+.5);
			mArgs2[0] = BitWidth.create(16);
			if (longvalues2) { mArgs2[1] = new Long(fNote); }
			else { mArgs2[1] = new Integer(fNote); }
			mVal=(Value)(mCreateKnown.invoke(null, mArgs2));
			state.setPort(9, mVal, DELAY);
			mArgs2[0] = BitWidth.ONE;
			if (longvalues2) { mArgs2[1] = new Long(1); }
			else { mArgs2[1] = new Integer(1); }
			mVal=(Value)(mCreateKnown.invoke(null, mArgs2));
			state.setPort(8, mVal, DELAY);
			}
		else
			{
			mArgs2[0] = BitWidth.ONE;
			if (longvalues2) { mArgs2[1] = new Long(0); }
			else { mArgs2[1] = new Integer(0); }
			mVal=(Value)(mCreateKnown.invoke(null, mArgs2));
			state.setPort(8, mVal, DELAY);
			}
		}
	catch (Throwable e) { System.err.println(e); }
	}

	private void updatePorts(Instance instance)
	{
		Port[] ps = new Port[11];
		/* ps[8] = new Port(0, 100, Port.INPUT, 1);
		ps[8].setToolTip(new MyStringGetter("Clock")); */
		ps[8] = new Port(0, 100, Port.OUTPUT, 1);
		ps[8].setToolTip(new MyStringGetter("Buzzer Enable"));
		ps[9] = new Port(0, 110, Port.OUTPUT, BitWidth.create(16));
		ps[9].setToolTip(new MyStringGetter("Buzzer Frequency"));
		ps[10] = new Port(0, 120, Port.INPUT, BitWidth.create(3));
		ps[10].setToolTip(new MyStringGetter("Channel"));
		for (int i=0; i<8; i++)
			{
			ps[i] = new Port(0, i*10+10, Port.OUTPUT, BitWidth.create(8));
			String label = "Channel "+(i+1)+" Note";
			ps[i].setToolTip(new MyStringGetter(label));
			}
		instance.setPorts(ps);
	}
	
	private static final int[] keys1 = {0,2,4,5,7,9,11};
	private static final int[] keys2 = {1,3,0,6,8,10,0};
	private static final Color[] noteCols = {Color.getHSBColor(0.0F,1.0F,1.0F),
									Color.getHSBColor(30.0F/360.0F,1.0F,1.0F),
									Color.getHSBColor(60.0F/360.0F,1.0F,1.0F),
									Color.getHSBColor(90.0F/360.0F,1.0F,1.0F),
									Color.getHSBColor(120.0F/360.0F,1.0F,1.0F),
									Color.getHSBColor(150.0F/360.0F,1.0F,1.0F),
									Color.getHSBColor(180.0F/360.0F,1.0F,1.0F),
									Color.getHSBColor(210.0F/360.0F,1.0F,1.0F),
									Color.getHSBColor(240.0F/360.0F,1.0F,1.0F),
									Color.getHSBColor(270.0F/360.0F,1.0F,1.0F),
									Color.getHSBColor(300.0F/360.0F,1.0F,1.0F),
									Color.getHSBColor(330.0F/360.0F,1.0F,1.0F)};

	public static class Poker extends InstancePoker
	{
		@Override
		public void mousePressed(InstanceState state, MouseEvent e)
		{
			//State val = (State)state.getData();
			Location loc = state.getInstance().getLocation();
			int cx = e.getX() - loc.getX();
			int cy = e.getY() - loc.getY();
			int i = cx / 20;
			int s = ((cx - 13)/20)%7;
			int sm = ((cx - 13)/20)/7;
			int oct = state.getAttributeValue(ATTR_OCTAVES);
			int extraC = (state.getAttributeValue(ATTR_EXTRAC)) ? 1 : 0 ;
			int dx = (7*oct+extraC)*20;
			int chanSel = state.getAttributeValue(ATTR_CHANSEL);
			if ((chanSel > 0) && (cy < 0) && (cy > -15))
				{
				int dx2 = dx/2-((chanSel*15)/2);
				if ((cx >= dx2) && (cx <= (chanSel*15+dx2)))
					{
					int chan = ((cx-dx2)/15);
					Channel chanNum = new Channel(chan);
					state.setData(chanNum);
					return;
					}
				}
			if ((cx >= dx) || (cx < 0) || (cy < 0)) return;
			/* these calculations could probably be improved */
			if ((cy < 60) && (cx <= (sm*20*7+s*20+26)) && (cx >= 13) && (sharps[s] == 1))
				{
				noteX = sm*20*7+s*20+16+loc.getX(); noteY = 1+loc.getY();
				noteW = 8; noteH = 57;
				noteVal = keys2[s];
				}
			else
				{
				noteX = i*20+1+loc.getX(); noteY = 1+loc.getY();
				noteW = 18; noteH = 128;
				noteVal = keys1[i%7];
				}
			if (colorNotes == 1) { noteColor = noteCols[noteVal]; }
			else { noteColor = Color.RED; }
			noteVal += (i/7)*12;
			state.getInstance().fireInvalidated();
		}
		
		@Override
		public void mouseDragged(InstanceState state, MouseEvent e)
		{
			//State val = (State)state.getData();
			Location loc = state.getInstance().getLocation();
			int cx = e.getX() - loc.getX();
			int cy = e.getY() - loc.getY();
			int i = cx / 20;
			int s = ((cx - 13)/20)%7;
			int sm = ((cx - 13)/20)/7;
			int oct = state.getAttributeValue(ATTR_OCTAVES);
			int extraC = (state.getAttributeValue(ATTR_EXTRAC)) ? 1 : 0 ;
			int dx = (7*oct+extraC)*20;
			if ((cx >= dx) || (cx < 0) || (cy < 0)) return;
			if ((cy < 60) && (cx <= (sm*20*7+s*20+26)) && (cx >= 13) && (sharps[s] == 1))
				{
				noteX = sm*20*7+s*20+16+loc.getX(); noteY = 1+loc.getY();
				noteW = 8; noteH = 57;
				noteVal = keys2[s];
				}
			else
				{
				noteX = i*20+1+loc.getX(); noteY = 1+loc.getY();
				noteW = 18; noteH = 128;
				noteVal = keys1[i%7];
				}
			if (colorNotes == 1) { noteColor = noteCols[noteVal]; }
			else { noteColor = Color.RED; }
			noteVal += (i/7)*12;
			state.getInstance().fireInvalidated();
		}
		
		@Override
		public void mouseReleased(InstanceState state, MouseEvent e)
		{
			noteVal = -1;
			noteX = -1;
			state.getInstance().fireInvalidated();
		}
	}

	public static class MyStringGetter implements StringGetter
	{
		private String str;

		public MyStringGetter(String str) { this.str = str; }

		public String get() { return str; }

		@Override
		public String toString() { return str; }
	}
}
