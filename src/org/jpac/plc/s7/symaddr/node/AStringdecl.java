/* This file was generated by SableCC (http://www.sablecc.org/). */

package org.jpac.plc.s7.symaddr.node;

import org.jpac.plc.s7.symaddr.analysis.*;

@SuppressWarnings("nls")
public final class AStringdecl extends PStringdecl
{
    private TString _string_;
    private TLSquarebracket _lSquarebracket_;
    private TNumber _number_;
    private TRSquarebracket _rSquarebracket_;

    public AStringdecl()
    {
        // Constructor
    }

    public AStringdecl(
        @SuppressWarnings("hiding") TString _string_,
        @SuppressWarnings("hiding") TLSquarebracket _lSquarebracket_,
        @SuppressWarnings("hiding") TNumber _number_,
        @SuppressWarnings("hiding") TRSquarebracket _rSquarebracket_)
    {
        // Constructor
        setString(_string_);

        setLSquarebracket(_lSquarebracket_);

        setNumber(_number_);

        setRSquarebracket(_rSquarebracket_);

    }

    @Override
    public Object clone()
    {
        return new AStringdecl(
            cloneNode(this._string_),
            cloneNode(this._lSquarebracket_),
            cloneNode(this._number_),
            cloneNode(this._rSquarebracket_));
    }

    public void apply(Switch sw)
    {
        ((Analysis) sw).caseAStringdecl(this);
    }

    public TString getString()
    {
        return this._string_;
    }

    public void setString(TString node)
    {
        if(this._string_ != null)
        {
            this._string_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._string_ = node;
    }

    public TLSquarebracket getLSquarebracket()
    {
        return this._lSquarebracket_;
    }

    public void setLSquarebracket(TLSquarebracket node)
    {
        if(this._lSquarebracket_ != null)
        {
            this._lSquarebracket_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._lSquarebracket_ = node;
    }

    public TNumber getNumber()
    {
        return this._number_;
    }

    public void setNumber(TNumber node)
    {
        if(this._number_ != null)
        {
            this._number_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._number_ = node;
    }

    public TRSquarebracket getRSquarebracket()
    {
        return this._rSquarebracket_;
    }

    public void setRSquarebracket(TRSquarebracket node)
    {
        if(this._rSquarebracket_ != null)
        {
            this._rSquarebracket_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._rSquarebracket_ = node;
    }

    @Override
    public String toString()
    {
        return ""
            + toString(this._string_)
            + toString(this._lSquarebracket_)
            + toString(this._number_)
            + toString(this._rSquarebracket_);
    }

    @Override
    void removeChild(@SuppressWarnings("unused") Node child)
    {
        // Remove child
        if(this._string_ == child)
        {
            this._string_ = null;
            return;
        }

        if(this._lSquarebracket_ == child)
        {
            this._lSquarebracket_ = null;
            return;
        }

        if(this._number_ == child)
        {
            this._number_ = null;
            return;
        }

        if(this._rSquarebracket_ == child)
        {
            this._rSquarebracket_ = null;
            return;
        }

        throw new RuntimeException("Not a child.");
    }

    @Override
    void replaceChild(@SuppressWarnings("unused") Node oldChild, @SuppressWarnings("unused") Node newChild)
    {
        // Replace child
        if(this._string_ == oldChild)
        {
            setString((TString) newChild);
            return;
        }

        if(this._lSquarebracket_ == oldChild)
        {
            setLSquarebracket((TLSquarebracket) newChild);
            return;
        }

        if(this._number_ == oldChild)
        {
            setNumber((TNumber) newChild);
            return;
        }

        if(this._rSquarebracket_ == oldChild)
        {
            setRSquarebracket((TRSquarebracket) newChild);
            return;
        }

        throw new RuntimeException("Not a child.");
    }
}
