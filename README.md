# HDL to IMG
Produces graphical representation of HDL component.
At the moment it is possible to generate SVGs out of VHDL components. 
A JSON configuration file represents how the output image is produced

## Examples

Let's say we have the following component declaration 

```vhdl
entity ExampleComp is
  port(
    io_a : in signed(3 downto 0);
    io_b : in signed(3 downto 0);
    io_c : inout signed(3 downto 0);
    io_d : out signed(31 downto 0);
    io_f : out std_logic;
    clk : in std_logic;
    reset : in std_logic
  );
end ExampleComp;

```

Here there are 

Serif font, Top Left component name, arrow style None: <br/>
![style1](./res/ExampleComp_1.svg)

Arial font, Top Center component name, arrow style Full: <br/>
![style2](./res/ExampleComp_2.svg)

FreeMono font, Top Right component name, arrow style Empty: <br/>
![style3](./res/ExampleComp_3.svg)

FreeSans font, Bottom Left component name, arrow style Reentrant: <br/>
![style4](./res/ExampleComp_4.svg)

Serif font, Bottom Right component name, arrow style Minimal: <br/>
![style5](./res/ExampleComp_5.svg)
<br/>
Here it is the JSON configuration file for the last example

```json
{
"figPadX": 8.0,
"figPadY": 8.0,
"compPadX": 4.0,
"compPadY": 4.0,
"compFont": {"font": "Serif", "weight": "BOLD", "size":11},
"compAlignment": ["Top", "Left"],
"boxTickness": 1.0,
"boxFillColor": [1.0, 1.0, 1.0],
"boxWidthPolicy": {"type": "BoxSigNamePad", "pad": 20.0},
"boxBorderPad": 4.0,
"sigPadX": 4.0,
"sigPadY": 4.0,
"sigFont": {"font": "Serif", "weight": "BOLD", "size":10},
"descPadX": 4.0,
"groupPadY": 10.0,
"groupPolicy": {"type": "SimplePolicy"},
"arrowLength": 30.0,
"arrowPadX": 2.0,
"arrowThickness": 2.0,
"arrowTipSize": 3.0,
"arrowStyle": "StyleEmpty",
"outputType": "SVGTypeOutput",
"removeDefaultInputArg": true
}

```

## Usage

If HDLtoIMG is not installed system wide you can use it through **sbt**:
```bash
    sbt "runMain com.github.hdltoimg.vhdl.VhdlToImg -i source.vhd -c conf.json"
```

otherwise you can simply type

```bash
    vhdl-to-img -i source.vhd -c conf.json
```

### Size mappings
<br/>
[comp](./res/ExampleCompExpl.svg)

maps to the following JSON properties: <br/>

[mappings](./res/JsonExplainedSizes.svg)


### Box width

The resulting component box size is determined by the **boxWidthPolicy** field

#### Constant width mode

The width of the box is constant

```json
    "boxWidthPolicy": {"type": "BoxConstantWidth", "width": 20.0}
```

#### Pad mode

The width of the box is the sum of a pad value plus the size of the signal names

```json
    "boxWidthPolicy": {"type": "BoxSigNamePad", "pad": 20.0}
```

#### Ratio mode

The width of the box is equal to the height multiplied by a ratio

```json
    "boxWidthPolicy": {"type": "BoxConstantRatio", "pad": 1.0}
```
.. for a square box


### Grouping

For components with a lot of ports sometimes the resulting image looks a bit difficult to read:

[ungrouped](./res/VexRiscv-ungrouped.svg)

The above example shows the interface of a CPU. It would be better to group the 
signals that belong to the same bus togheter. This is possible using the 
**RegexGroupsPolicy**. With this policy the signals are grouped using a regex
(**groupRegex**) that matches the group name.

```json
    "groupPolicy": {"type": "RegexGroupsPolicy", "groupRegex": "^([^_]*).*"}
```

That produces:

[grouped](./res/VexRiscv-grouped.svg)

### Font weight

There are four options:

- PLAIN
- BOLD
- ITALIC
- BOLDITALIC


## Installation Requirements

- SBT
- Java JRE
- Java JDK
- zip

### Debian

- dpkg-deb
- dpkg-sig
- dpkg-genchanges
- lintian
- fakeroot

### RedHat

- rpm
- rpm-build

### Windows

- [WIX Toolset](https://wixtoolset.org/)

## Installation

### General

```bash
    sbt universal:packageBin
```

Generates a .zip in target/

### Debian
```bash
    sbt debian:packageBin
```

Generates a .deb in target/

### RedHat
```bash
    sbt rpm:packageBin
```

Generates a .rpm in target/

### Windows

Before launching the compilation command two values have to be adjusted in
build.sbt to your Wix Toolset installation: **wixProductId**, **wixProductUpgradeId**

```bash
    sbt windows:packageBin
```

Generates a .msi in target/

## Notes

All the HDL examples were produced using [SpinalHDL](https://github.com/SpinalHDL/SpinalHDL)
