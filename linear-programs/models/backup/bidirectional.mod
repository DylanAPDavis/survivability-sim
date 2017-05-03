# GIVEN

# Set of Vertices
set V;

# All pairs of nodes
set AllPairs = {V,V};

# Maximum Number of Connections
param I_max >= 0 integer;

# Set of Connection Indices
set I := 1..I_max;			

# Physical Links - 1 if Link exists between u and v
param A{u in V, v in V} binary;

# S - Sources
set S;	

# D - Destinations
set D;

# F - Failure Matrix
set F within AllPairs;

# c - Number of connections that need to survive k failures
param c >= 0 integer;

# k - Number of failures that need to be survived
param k >= 0 integer;

# NumGroups - Number of k-sized failure groups
param NumGroups;

# GroupIndices - Indexing set for all groups of failure elements of size k
set GroupIndices := 1..NumGroups;

# FG - Set of all failure groups of size k
set FG {g in GroupIndices} within AllPairs;


# VARIABLES

# C - connection number (i) from node s to node d
var C{s in V, d in V, i in I} binary;

# L - Link flow on (u, v) used by Connection (s, d, i)
var L{s in V, d in V, i in I, u in V, v in V} binary;

# FC - Number of failed connections caused by node v / link u, v
var FC{u in V, v in V} >= 0 integer;

# NC - Node v is in Connection (s,d,i)
var NC{s in V, d in V, i in I, v in V} binary;

# FG_Sum - Number of failed connections caused by this failure group (groupIndex)
var FG_Sum {g in GroupIndices} >= 0 integer;

# Number of connections
var Num_Conn = sum{s in S, d in D, i in I} C[s,d,i];

# Number of reverse connections
var Num_Rev_Conn = sum{d in D, s in S, i in I} C[d,s,i];

# Number of non Src-Dst connections
var Num_Non_Src_Dest_Conn = sum{u in V, v in V, i in I: not (u in S and v in D or u in D and v in S)} C[u,v,i];

# Number of link usages
var Num_Link_Usages = sum{s in S, d in D, i in I, u in V, v in V} L[s,d,i,u,v];


# OBJECTIVE

minimize Objective:
	Num_Link_Usages;

## Connection Constraints

subject to numConnectionsNeeded{g in GroupIndices}:
	Num_Conn >= c + FG_Sum[g];

subject to numReverseConnectionsNeeded{g in GroupIndices}:
	Num_Rev_Conn = Num_Conn;

subject to numNonSrcDestConnectionsNeeded:
	Num_Non_Src_Dest_Conn = 0;

subject to reverseConnectionIfForward{s in V, d in V, i in I}:
	C[s,d,i] = C[d,s,i];

subject to flowOnlyIfConnectionAndLinkExists{s in V, d in V, i in I, u in V, v in V}:
	L[s,d,i,u,v] <= A[u,v] * C[s,d,i];

subject to intermediateFlow{s in V, d in V, i in I, v in V: v != s and v != d}:
	sum{u in V} L[s,d,i,u,v] - sum{w in V} L[s,d,i,v,w] = 0;

subject to sourceFlow{s in V, d in V, i in I}:
	sum{u in V} L[s,d,i,u,s] - sum{w in V} L[s,d,i,s,w] = -1 * C[s,d,i];

subject to destinationFlow{s in V, d in V, i in I}:
	sum{u in V} L[s,d,i,u,d] - sum{w in V} L[s,d,i,d,w] = C[s,d,i];	

subject to flowOnlyInConnection{s in V, d in V, i in I, u in V, v in V}:
	L[s,d,i,u,v] <= C[s,d,i];

subject to noFlowIntoSource{s in V, d in V, i in I, u in V}:
	L[s,d,i,u,s] = 0;

# Node is in a connection
subject to nodeInConnection_A{s in V, d in V, i in I, v in V}:
	NC[s,d,i,v] <= sum{u in V} L[s,d,i,u,v] + sum{w in V} L[s,d,i,v,w];

subject to nodeInConnection_B{s in V, d in V, i in I, v in V}:
	NC[s,d,i,v] * card(V)^4 >= sum{u in V} L[s,d,i,u,v] + sum{w in V} L[s,d,i,v,w];

## Failure Constraints

# Number of failures caused by a link --> Number of connections that include that element
subject to numFailedConnectionsLink{u in V, v in V :(u,v) in F and u != v}:
	FC[u, v] = sum{s in V, d in V, i in I} L[s,d,i,u,v];

# Number of failures caused by a node
subject to numFailedConnectionsNode{(u in V, v in V: (u,v) in F and u == v}:
	FC[u,v] = sum{s in V, d in V, i in I} NC[s,d,i,v];

# If a link fails in one direction, it fails in the other direction as well
subject to bidirectionalFailures{(u,v) in F}:
	FC[v,u] = FC[u,v];

# Sum up the failures per failure group
subject to totalFailuresPerGroup{g in GroupIndices}:
	FG_Sum[g] = sum{(u, v) in FG[g]} FC[u,v];