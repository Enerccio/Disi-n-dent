#!/bin/bash
set -xv

TMP="/tmp/pbtbsparser/"
SRC="main/antlr/cz/upol/vanusanik/disindent/parser/disindent.g4"
OUT="src/generated/antlr"
PACKAGE="main.antlr.cz.upol.vanusanik.disindent.parser"
ANTLR="antlr4"
GRUN="grun"

if [[ $(uname) == MINGW* ]] ; then
	export CLASSPATH="$(pwd -W)/lib/antlr-4.5-complete.jar;$CLASSPATH"
	ANTLR="java -Xmx500M org.antlr.v4.Tool"
	GRUN="java org.antlr.v4.runtime.misc.TestRig"
else
	export CLASSPATH="/usr/share/java/antlr-complete.jar:$CLASSPATH"
fi

rm $OUT/*.token -rv
rm $OUT/*.java -rv
mkdir $OUT
pushd src
$ANTLR -o ../$OUT -package "$PACKAGE" "$SRC" || exit 1
popd

# cp test/x.py $TMP/x.py
# cd $TMP

# $GRUN me.enerccio.sp.parser.python file_input -encoding 'utf-8' $1 x.py
