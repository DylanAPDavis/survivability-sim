% Set the paths to AMPLAPI JAR and matlab wrappers
function setUp()
  base = fileparts(which('setUp'));

  amplname = 'ampl-1.2.2.jar';
  libname = 'matlablib-1.2.2.jar';

  % Check for file existance
  api = fullfile(base, '..', 'lib', amplname);
  matlabapi = fullfile(base, '..', 'matlab', libname);

  if(fileExists(api)~=0)
    error(['Cannot find AMPL API in ', api])
  end
  if(fileExists(matlabapi)~=0)
    error(['Cannot find Matlab API helper in ', matlabapi])
  end

  javaaddpath(api);
  javaaddpath(matlabapi);
  addpath(base);    

  disp(['Using wrapper at ', base])
  disp(['Using api at ', api])
end
