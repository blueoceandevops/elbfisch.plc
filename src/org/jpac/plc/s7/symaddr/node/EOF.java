/* This file was generated by SableCC (http://www.sablecc.org/). */

package org.jpac.plc.s7.symaddr.node;

import org.jpac.plc.s7.symaddr.analysis.*;

@SuppressWarnings("nls")
public final class EOF extends Token
{
    public EOF()
    {
        setText("");
    }

    public EOF(int line, int pos)
    {
        setText("");
        setLine(line);
        setPos(pos);
    }

    @Override
    public Object clone()
    {
        return new EOF(getLine(), getPos());
    }

    public void apply(Switch sw)
    {
        ((Analysis) sw).caseEOF(this);
    }
}