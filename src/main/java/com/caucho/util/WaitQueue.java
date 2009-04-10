/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.util;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

/**
 * wait queue
 */
public class WaitQueue {
  private volatile Item _head;

  public boolean wake()
  {
    for (Item item = _head; item != null; item = item.getNext()) {
      if (item.unpark())
	return true;
    }

    return false;
  }

  public void wakeAll()
  {
    for (Item item = _head; item != null; item = item.getNext()) {
      item.unpark();
    }
  }

  public Item create()
  {
    Item item = new Item();

    synchronized (this) {
      item.setNext(_head);
      _head = item;
    }

    return item;
  }

  private void remove(Item item)
  {
    synchronized (this) {
      Item prev = null;

      for (Item ptr = _head; ptr != null; ptr = ptr.getNext()) {
	if (ptr == item) {
	  if (prev != null) {
	    prev.setNext(ptr.getNext());
	  }
	  else {
	    _head = ptr.getNext();
	  }
	}
      }
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }

  public final class Item {
    private final Thread _thread;

    // private final AtomicReference<Item> _next = new AtomicReference<Item>();
    private Item _next;
    private boolean _isParked;

    Item()
    {
      _thread = Thread.currentThread();
    }

    final Thread getThread()
    {
      return _thread;
    }

    final Item getNext()
    {
      // return _next.get();
      return _next;
    }

    final void setNext(Item next)
    {
      _next = next;
      // _next.set(next);
    }

    final boolean unpark()
    {
      boolean isParked = _isParked;

      LockSupport.unpark(_thread);
	
      return isParked;
    }

    public final void park(long millis)
    {
      _isParked = true;

      Thread.interrupted();
      LockSupport.parkNanos(millis * 1000L);
      
      _isParked = false;
    }

    public void remove()
    {
      WaitQueue.this.remove(this);
    }
  }
}
