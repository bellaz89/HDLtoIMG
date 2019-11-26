#!/usr/bin/env sh

for i in 1 2 3 4 5
do 
        vhdl-to-img -i ExampleComp.vhd -c style$i.json
        mv ExampleComp.svg ExampleComp_$i.svg
done

vhdl-to-img -i VexRiscv.vhd -c style4.json
mv VexRiscv.svg VexRiscv-ungrouped.svg
vhdl-to-img -i VexRiscv.vhd -c style6.json
mv VexRiscv.svg VexRiscv-grouped.svg
vhdl-to-img -i ExampleComp.vhd -c style7.json
