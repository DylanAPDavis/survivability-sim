#ifndef AMPL_OUTPUT_H_
#define AMPL_OUTPUT_H_

namespace ampl {

namespace output {
/*!
Represents the type of the output coming from the interpreter
*/
enum Kind

{
  /**
  * Output ``prompt2``, returned when incomplete statements are
  * interpreted
  */
  WAITING,

  /**
  * Output ``break``, displayed when an operation is interrupted with
  * SIGINT
  */
  BREAK,
  /**
  * Output ``cd``, returned by the ``cd`` function.
  */
  CD,
  /**
  * Output ``display``, returned by the ``display`` function.
  */
  DISPLAY,
  /**
  * Output ``exit``, returned as last message from %AMPL before
  * exiting the interpreter
  */
  EXIT,
  /**
  * Output ``expand``, returned by the ``expand`` function.
  */
  EXPAND,
  /**
  * Output ``load``, returned by the ``load`` function when loading a
  * library
  */
  LOAD,
  /**
  * Output ``option``, returned by the ``option`` function when
  * getting the value of an option
  */
  OPTION,
  /**
  * Output ``print``, returned by the ``print`` function when
  * printing values from %AMPL command line
  */
  PRINT,
  /**
  * Output ``prompt1``, normal %AMPL prompt
  */
  PROMPT,  // prompt1 and prompt3
       /**
       * Output ``solve``, returned by the ``solve`` function, contains
       * the solver message
       */
  SOLVE,
  /**
  * Output ``show``, returned by the ``show`` function
  */
  SHOW,
  /**
  * Output ``xref``, returned by the ``xref`` function.
  */
  XREF,
  /**
  * Output of the %AMPL command ``shell``
  */
  SHELL_OUTPUT,
  /**
  * Messages from the command ``shell``
  */
  SHELL_MESSAGE,
  /**
  * Output ``misc``
  */
  MISC,
  /**
  * Messages from the command ``write table``
  */
  WRITE_TABLE
};
}

/**
* Implement this interface to handle the outputs from the calls to any
* function that causes the underlying %AMPL interpreter to display a message.
* <p>
* Note that errors and warnings are not passed through this interface,
* see ampl::ErrorHandler for more information.
*/
class OutputHandler {
public:
  OutputHandler() {}
  virtual ~OutputHandler() {}
  /**
  * This method is called when %AMPL sends some output derived from executing a statement
  * @param kind kind of the output of the %AMPL interpreter
  * @param output text of the output by the %AMPL interpreter
  */
  virtual void output(output::Kind kind, const char* output) = 0;
};

namespace internal {
  typedef void(*OutputHandlerCbPtr)(output::Kind, const char*, void*);
}
}

#endif  // AMPL_OUTPUT_H_
