#ifndef AMPL_ENTITY_EP_H
#define AMPL_ENTITY_EP_H

#include "ampl/ep/arg.h"
#include "ampl/ep/scopedarray.h" // for PODStringRef
#include "ampl/tuple.h"
#include "ampl/ep/dataframe_ep.h"
#include "ampl/ep/declarations.h"

#include <map>

namespace ampl {

namespace internal {

  // Forward declarations
  class EntityBase;
  class Variable;
  class Constraint;
  class Set;
  class Objective;
  class Parameter;
  class Instance;

  struct Entity_RefPointer {
    std::size_t count;
    std::map<TupleRef, Instance *>::iterator it;
  };

  struct Parameter_RefPointer {
    std::size_t count;
    std::map<ampl::Tuple, ampl::Variant>::iterator it;
  };

  extern "C" {
    AMPLAPI void AMPL_EntityBase_Destroy(EntityBase *impl);
    AMPLAPI const char *AMPL_EntityBase_name(EntityBase *impl);
    AMPLAPI int AMPL_EntityBase_indexarity(EntityBase *impl);
    AMPLAPI const char **
      AMPL_EntityBase_getIndexingSets(EntityBase *impl, std::size_t *n,
        ErrorInformation *errorInfo);

    
    AMPLAPI void AMPL_EntityBase_getValues(EntityBase *impl,
      const char *const *suffixes,
      int num_suffixes, DataFrame* output,
      ErrorInformation *errorInfo);

    AMPLAPI void AMPL_EntityBase_setValues(EntityBase *impl, 
      DataFrame* data, ErrorInformation *errorInfo);

    AMPLAPI const char *AMPL_EntityBase_toString(EntityBase *impl);

    // ************* Map related *************
    AMPLAPI Instance *AMPL_Entity_getgeneric_tuple(EntityBase *impl, Tuple t,
      ErrorInformation *errorInfo);
    AMPLAPI std::size_t AMPL_Entity_numInstances(EntityBase *impl,
      ErrorInformation *errorInfo);

    // ************* Iterators *************
    AMPLAPI Tuple  AMPL_Entity_iterator_getPointer(Entity_RefPointer *it1,
      Instance** instance);
    AMPLAPI Entity_RefPointer *
      AMPL_Entity_begin(EntityBase *impl, ErrorInformation *errorInfo);
    AMPLAPI Entity_RefPointer *
      AMPL_Entity_end(EntityBase *impl, ErrorInformation *errorInfo);
    AMPLAPI Entity_RefPointer *
      AMPL_Entity_find(EntityBase *impl, Tuple t,
        ErrorInformation *errorInfo);
    AMPLAPI void AMPL_Entity_iterator_increment(Entity_RefPointer *it,
      ErrorInformation *errorInfo);
    AMPLAPI void AMPL_Entity_iterator_delete(Entity_RefPointer *it);
    AMPLAPI bool AMPL_Entity_iterator_equal(Entity_RefPointer *it1,
      Entity_RefPointer *it2,
      ErrorInformation *errorInfo);

    // ************* Variable *************
    AMPLAPI int AMPL_Variable_integrality(Variable *impl,
      ErrorInformation *errorInfo);
    AMPLAPI void AMPL_Variable_fix(Variable *impl, ErrorInformation *errorInfo);
    AMPLAPI void AMPL_Variable_fix_value(Variable *impl, double value,
      ErrorInformation *errorInfo);
    AMPLAPI void AMPL_Variable_unfix(Variable *impl, ErrorInformation *errorInfo);
    AMPLAPI void AMPL_Variable_setValue(Variable *impl, double value,
      ErrorInformation *errorInfo);

    // *************  Parameter *************
    AMPLAPI void AMPL_Parameter_setValues(Parameter *impl, Tuple indices[],
      AMPLArgs values, std::size_t nvalues,
      ErrorInformation *errorInfo);

    AMPLAPI void AMPL_Parameter_setValuesMatrix(Parameter *impl, std::size_t nr,
      AMPLArgs rows, std::size_t nc,
      AMPLArgs cols,
      const double *values,
      bool transpose,
      ErrorInformation *errorInfo);
    AMPLAPI void AMPL_Parameter_setValuesMatrix_str(Parameter *impl, std::size_t nr,
      AMPLArgs rows, std::size_t nc,
      AMPLArgs cols,
      const char *const *values,
      bool transpose,
      ErrorInformation *errorInfo);
    AMPLAPI void AMPL_Parameter_setValuesOrdered(Parameter *impl, AMPLArgs values,
      std::size_t n,
      ErrorInformation *errorInfo);
    AMPLAPI void AMPL_Parameter_setScalar(Parameter *impl, Variant value,
      ErrorInformation *errorInfo);
    AMPLAPI void AMPL_Parameter_set(Parameter *impl, Tuple index, Variant value,
      ErrorInformation *errorInfo);
    AMPLAPI bool AMPL_Parameter_isSymbolic(Parameter *impl);
    AMPLAPI bool AMPL_Parameter_hasDefault(Parameter *impl);

    AMPLAPI Variant AMPL_Parameter_getgeneric_tuple(Parameter *impl, Tuple t,
      ErrorInformation *errorInfo);
    // ************* Iterators *************
    AMPLAPI Variant AMPL_Parameter_iterator_getPointer(Parameter_RefPointer *it1, Tuple *t);
    AMPLAPI Parameter_RefPointer *
      AMPL_Parameter_begin(Parameter *impl, ErrorInformation *errorInfo);
    AMPLAPI Parameter_RefPointer *
      AMPL_Parameter_end(Parameter *impl, ErrorInformation *errorInfo);
    AMPLAPI Parameter_RefPointer *
      AMPL_Parameter_find(Parameter *impl, Tuple t,
        ErrorInformation *errorInfo);
    AMPLAPI void AMPL_Parameter_iterator_increment(Parameter_RefPointer *it,
      ErrorInformation *errorInfo);
    AMPLAPI void AMPL_Parameter_iterator_delete(Parameter_RefPointer *it);
    AMPLAPI bool AMPL_Parameter_iterator_equal(Parameter_RefPointer *it1,
      Parameter_RefPointer *it2,
      ErrorInformation *errorInfo);

    // *************  Objective *************
    AMPLAPI int AMPL_Objective_sense(Objective *impl,
      ErrorInformation *errorInfo);
    AMPLAPI void AMPL_Objective_restore(Objective *impl,
      ErrorInformation *errorInfo);
    AMPLAPI void AMPL_Objective_drop(Objective *impl, ErrorInformation *errorInfo);
    // ***************** Set ****************
    AMPLAPI std::size_t AMPL_Set_arity(Set *impl);
    // ************* Constraint *************
    AMPLAPI bool AMPL_Constraint_isLogical(Constraint *impl,
      ErrorInformation *errorInfo);
    AMPLAPI void AMPL_Constraint_setDual(Constraint *impl, double dual,
      ErrorInformation *errorInfo);
    AMPLAPI void AMPL_Constraint_drop(Constraint *impl,
      ErrorInformation *errorInfo);
    AMPLAPI void AMPL_Constraint_restore(Constraint *impl,
      ErrorInformation *errorInfo);
  } // extern C



  // Template class which wraps all the procedures regarding instance access
  // and iterators for all Entities (besides Parameter)
  template <class InstanceClass> class EntityWrapper {
    typedef Instance *InstancePointer;
  public:
    typedef typename InstanceClass::Inner Inner;
    typedef std::pair<TupleRef, InstanceClass> ValueType;
    typedef internal::Entity_RefPointer *ReferencePointer;

    static void dispose(ReferencePointer data) {
      internal::AMPL_Entity_iterator_delete(data);
    }

    static bool equal(ReferencePointer it1, ReferencePointer it2) {
      return AMPL_Entity_iterator_equal(it1, it2, internal::ErrorInfo());
    }
    static void increment(ReferencePointer it) {
      AMPL_Entity_iterator_increment(it, internal::ErrorInfo());
    }
    static ValueType getPointer(ReferencePointer it) {
      Instance* instance;
      Tuple t = internal::AMPL_Entity_iterator_getPointer(it, &instance);
      return ValueType(TupleRef(t), InstanceClass(instance));
    }

    static InstancePointer get(EntityBase *impl, Tuple t,
      ErrorInformation *errorInfo) {
      return AMPL_Entity_getgeneric_tuple(impl, t, errorInfo);
    }

    static ReferencePointer begin(EntityBase *impl) {
      return AMPL_Entity_begin(impl, internal::ErrorInfo());
    }

    static ReferencePointer end(EntityBase *impl) {
      return AMPL_Entity_end(impl, internal::ErrorInfo());
    }
    static ReferencePointer find(EntityBase *impl, Tuple t) {
      return AMPL_Entity_find(impl, t, internal::ErrorInfo());
    }
  };

  // Template specialization which wraps all the procedures regarding instance access
  // and iterators for Parameter
  template <> class EntityWrapper <VariantRef> {
    typedef Variant InstancePointer;
  public:
    typedef internal::Variant Inner;
    typedef std::pair<TupleRef, ampl::VariantRef> ValueType;
    typedef internal::Parameter_RefPointer *ReferencePointer;

    static void dispose(ReferencePointer data) {
      internal::AMPL_Parameter_iterator_delete(data);
    }

    static bool equal(ReferencePointer it1, ReferencePointer it2) {
      return AMPL_Parameter_iterator_equal(it1, it2, internal::ErrorInfo());
    }
    static void increment(ReferencePointer it) {
      AMPL_Parameter_iterator_increment(it, internal::ErrorInfo());
    }

    static ValueType getPointer(ReferencePointer it) {
      Tuple t;
      Variant v = internal::AMPL_Parameter_iterator_getPointer(it, &t);
      return ValueType(TupleRef(t), VariantRef(v));
    }

    static InstancePointer get(EntityBase *impl, Tuple t,
      ErrorInformation *errorInfo) {
      return AMPL_Parameter_getgeneric_tuple(
        reinterpret_cast<internal::Parameter *>(impl), t, errorInfo);
    }

    static ReferencePointer begin(EntityBase *impl) {
      return AMPL_Parameter_begin(
        reinterpret_cast<internal::Parameter *>(impl), internal::ErrorInfo());
    }

    static ReferencePointer end(EntityBase *impl) {
      return AMPL_Parameter_end(
        reinterpret_cast<internal::Parameter *>(impl), internal::ErrorInfo());
    }
    static ReferencePointer find(EntityBase *impl, Tuple t) {
      return AMPL_Parameter_find(
        reinterpret_cast<internal::Parameter *>(impl), t,
        internal::ErrorInfo());
    }
  };
} // namespace internal
} // namespace ampl

#endif // AMPL_ENTITY_EP_H
