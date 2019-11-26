-- Generator : SpinalHDL v1.3.6    git head : 9bf01e7f360e003fac1dd5ca8b8f4bffec0e52b8
-- Date      : 08/11/2019, 17:53:57
-- Component : VexRiscv

-- ..
-- Only the entity declaration is necessary

entity VexRiscv is
  port(
    timerInterrupt : in std_logic;
    externalInterrupt : in std_logic;
    softwareInterrupt : in std_logic;
    debug_bus_cmd_valid : in std_logic;
    debug_bus_cmd_ready : out std_logic;
    debug_bus_cmd_payload_wr : in std_logic;
    debug_bus_cmd_payload_address : in unsigned(7 downto 0);
    debug_bus_cmd_payload_data : in std_logic_vector(31 downto 0);
    debug_bus_rsp_data : out std_logic_vector(31 downto 0);
    debug_resetOut : out std_logic;
    iBus_cmd_valid : out std_logic;
    iBus_cmd_ready : in std_logic;
    iBus_cmd_payload_address : out unsigned(31 downto 0);
    iBus_cmd_payload_size : out unsigned(2 downto 0);
    iBus_rsp_valid : in std_logic;
    iBus_rsp_payload_data : in std_logic_vector(31 downto 0);
    iBus_rsp_payload_error : in std_logic;
    dBus_cmd_valid : out std_logic;
    dBus_cmd_ready : in std_logic;
    dBus_cmd_payload_wr : out std_logic;
    dBus_cmd_payload_address : out unsigned(31 downto 0);
    dBus_cmd_payload_data : out std_logic_vector(31 downto 0);
    dBus_cmd_payload_mask : out std_logic_vector(3 downto 0);
    dBus_cmd_payload_length : out unsigned(2 downto 0);
    dBus_cmd_payload_last : out std_logic;
    dBus_rsp_valid : in std_logic;
    dBus_rsp_payload_data : in std_logic_vector(31 downto 0);
    dBus_rsp_payload_error : in std_logic;
    clk : in std_logic;
    reset : in std_logic;
    debugReset : in std_logic
  );
end VexRiscv;

