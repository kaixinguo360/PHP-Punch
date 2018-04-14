<?php
class Proxy {
    
    /* 成员变量 */
    var $address, $socket, $name;
    var $status, $lastHB;
    var $target, $targetPort;
    var $selfPort;
    
    /* 构造函数 */
    function __construct($socket1, $address1) {
        $this -> address = $address1;
        $this -> name = $address1;
        $this -> socket = $socket1;
        $this -> status = 0;
        $this -> lastHB =time();
        $this -> mylog("Host " . $this -> address . " Connect...");
    }
    
    /* 成员函数 */
    function receive($data){
        $this -> mylog($this -> name . " --> " . $data, 1);
        $this -> lastHB = time();
        
        if($data == "SYN" && $this -> status != 0) {
            $this -> status = 0;
        } else if($this -> status == -1) {
            $this -> send("RETRY");
        } else if($this -> status == 0) {
            if($data == "SYN") {
                $this -> status = 1;
                $this -> send("ACK");
            } else {
                $this -> status = -1;
            }
        } else if($this -> status == 1) {
            $str = substr($data,0, 3);
            if($str == "ACK") {
                $this -> name = substr($data,3);
                $this -> status = 2;
                $this -> send("ACCEPT");
                $this -> mylog($this -> name . " >>> Login!");
            } else {
                $this -> status = -1;
            }
        } else if($this -> status >= 2) {
            if($data != "HB") {
                $this -> toDo($data);
            }
        }
    }
    
    function toDo($data){
        global $proxys;
        if($this -> status >= 3) {
            $this -> prepare($data);
        } else if($data == "LIST") {
            $this -> send(getList());
        } else if(substr($data,0, 3) == "CNT") {
            $this -> status = 3;
            $this -> prepare($data);
            
        }
    }
    
    function prepare($data){
        if($data == "HB") {
            return;
        } else if(substr($data, 0, 3) != "CNT") {
            $this -> status = 2;
            $this -> send("NON");
            return;
        }
        $data = substr($data, 3);
        if($this -> status >= 5) {
            $this -> p2p($data);
        } else if($this -> status == 3) {
            $this -> target = getProxy($data);
            if($this -> target) {
                $info = $this -> target -> connect($this);
                $this -> send("CNTACCEPT:" . $info);
                $this -> status = 4;
                $this -> mylog($this -> name . " > > Try CNT " . $data);
            } else {
                $this -> status = 2;
                $this -> send("NON");
            }
        } else if($this -> status == 4) {
            if(substr($data, 0, 4) == "PORT") {
                $this -> selfPort = substr($data, 4);
                if($this -> target -> selfPort) {
                    $this -> status = 5;
                    $this -> send("CNTOK" . $this -> selfPort);
                }
            } else {
                $this -> status = 2;
                $this -> send("NON");
            }
        }
    }
    
    function p2p($data){
        
    }
    
    function connect($porxy){
        $data = $this -> name . ":" . $this -> address;
        
        return $data;
    }
    
    function check(){
        $time = time() - $this -> lastHB;
        
        if($time < 15) {
            return true;
        } else {
            return false;
        }
    }
    
    function send($data){
        $this -> mylog($this -> name . " <-- " . $data, 1);
        stream_socket_sendto($this -> socket, $data, 0, $this -> address);
    }
    
    function mylog($data, $p = 0){
        if($p < LOG_LEVEL) {
            mylog($data);
        }
    }
}
