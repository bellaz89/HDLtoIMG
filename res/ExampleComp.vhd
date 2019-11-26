-- Generator : SpinalHDL v1.3.6    git head : 9bf01e7f360e003fac1dd5ca8b8f4bffec0e52b8
-- Date      : 25/11/2019, 09:16:20
-- Component : SumReg

-- ..
-- Only the entity declaration was kept..
--

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

