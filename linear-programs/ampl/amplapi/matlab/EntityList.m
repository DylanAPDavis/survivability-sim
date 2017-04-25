classdef EntityList < EntityListBase

methods
     function result = subsref(self, key)
       switch key(1).type
            case '()'
                if(length(key) < 2)
                    if(length(key.subs) == 1)
                        if(key.subs{1} > self.impl.size())
                            error('EntityList:subref', 'Index out of range')
                        else
                            entity = self.impl.get(key.subs{1}-1);
                            if(isa(entity, 'com.ampl.Variable'))
                                result = Variable(entity);
                            elseif(isa(entity, 'com.ampl.Constraint'))
                                result = Constraint(entity);
                            elseif(isa(entity, 'com.ampl.Parameter'))
                                result = Parameter(entity);
                            elseif(isa(entity, 'com.ampl.Objective'))
                                result = Objective(entity);
                            elseif(isa(entity, 'com.ampl.Set'))
                                result = Set(entity);
                            else
                                result = entity;
                            end                                
                        end
                    else
                       error('Entity:subsref', 'Not a supported subscripted reference')
                    end
                    return
                else
                    result = builtin('subsref',selccf,key);
                    return
                end 
            case '.'
                result = builtin('subsref',self,key);
                return
            case '{}'
                 error('Entity:subsref', 'Not a supported subscripted reference')

       end
     end
   
     
  function obj = EntityList(impl)
          obj@EntityListBase(impl)
  end  
  function display(self)
      self.impl.toString()
  end
  
  
  
end
end
