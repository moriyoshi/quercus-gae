<?php

class FilterIterator extends IteratorIterator {
  function __construct($iter)
  {
    parent::__construct($iter);
  }

  function accept()
  {
    return true;
  }

  function fetch()
  {
    for (; $this->it->valid() && ! $this->accept(); $this->it->next()) {
    }
  }

  function next()
  {
    parent::next();
    $this->fetch();
  }    

  function rewind()
  {
    parent::rewind();
    $this->fetch();
  }

  function __call($fun, $param)
  {
    return call_user_func_array(array($this->it, $fun), $param);
  }
}
