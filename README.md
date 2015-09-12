# Insight Engineering Platform

[![Build Status](https://travis-ci.org/Netflix/iep.svg)](https://travis-ci.org/Netflix/iep/builds)

Experiment for:

* Breaking out parts of the legacy monolithic platform library into simple modules so we can
  pick and choose the parts we want. See the readme for individual modules to get more
  information.
* Ensuring we can run well internally using the open source platform libraries. Many of these
  were open sourced by pulling out the core and having internal wrappers. Unfortunately that means
  it is unclear at this point what is in the open source library and what is provided in the
  wrapper layers. There are also some places where they have essentially forked.

## Running Internally

For an example of running internally see [iep-helloworld](http://go/iep-helloworld).

## Running Externally

We'll be trying this using the [zero to cloud](https://github.com/brharrington/zerotocloud)
project as an example. It is not fully fleshed out at this time.
