<?php
class Proxy {
    
    /* 成员变量 */
    var $address, $socket, $name;
    var $status, $lastHB;
    var $target;
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
                $this -> mylog("Host " . $this -> address . " >>> " . $this -> name);
                //$this -> mylog($this -> name . " >>> Login! (" . $this -> address . ")");
            } else {
                $this -> status = -1;
            }
        } else if($this -> status >= 2) {
            if(substr($data,0, 3) == "ACK") {
                $this -> send("ACCEPT");
            } else if($data != "HB") {
                $this -> toDo($data);
            }
        }
    }
    
    function toDo($data){
        global $proxys;
        if($this -> status >= 3) {
            $this -> prepare($data);
        } else if($data == "LIST") {
            $this -> send("LIST" . getList());
        } else if(substr($data,0, 3) == "CNT") {
            $this -> status = 3;
            $this -> prepare($data);
        }
    }
    
    function prepare($data){
        if($data == "HB") {
            return;
        } else if(substr($data, 0, 3) != "CNT") {
            $this -> disConnect();
            return;
        } else if(substr($data, 0, 3) == "NON") {
            $this -> disConnect();
            return;
        }
        $data = substr($data, 3);
        if($this -> status == 3) {
            $target = getProxy($data);
            if($target && ($target -> target == NULL || $target -> target == $this)) {
                $info = $target -> touch($this);
                $this -> target = $target;
                $this -> mylog($this -> name . " > > " . $data . " Try...");
                if($target -> target == $this) {
                    $this -> send("CNTACCEPT:" . $info);
                    $this -> status = 4;
                    $this -> mylog($this -> name . " >*> " . $data . " Accpet...");
                }
            } else {
                $this -> disConnect();
            }
        } else if($this -> status == 4) {
            if(substr($data, 0, 4) == "PORT") {
                $this -> selfPort = substr($data, 4);
                if($this -> target -> selfPort) {
                    $this -> status = 5;
                    $this -> send("CNTOK" . $this -> target -> selfPort);
                    $this -> mylog($this -> name . " >-> " . $this -> target -> name . " Prepared!");
                }
            } else {
                $this -> disConnect();
            }
        } else if($this -> status == 5) {
            if(substr($data, 0, 4) == "PORT") {
                if($this -> target -> selfPort) {
                    $this -> send("CNTOK" . $this -> target -> selfPort);
                }
            }
        }
    }
    
    function touch($porxy){
        if($this -> status == 2) {
            $this -> send("REQ" . $porxy -> name);
        }
        
        $data = $this -> name . ":" . $this -> address;
        return $data;
    }
    
    function disConnect(){
        $this -> status = 2;
        $this -> send("NON");
        $this -> target = NULL;
        $this -> selfPort = NULL;
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
