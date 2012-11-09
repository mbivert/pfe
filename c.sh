#!/bin/sh

if [ "$1" = "" ]; then a=rapport; else a=$1; fi

rm $a.pdf 2>/dev/null

pdflatex $a.tex
bibtex $a.aux
pdflatex $a.tex
pdflatex $a.tex

rm $a.toc $a.log $a.aux $a.bbl $a.blg 2>/dev/null
