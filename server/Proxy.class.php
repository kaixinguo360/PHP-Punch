<?php
class Proxy {
    
    /* 成员变量 */
    var $address, $socket, $name;
    var $status, $lastHB;
    
    /* 构造函数 */
    function __construct($socket1, $address1) {
        $this -> address = $address1;
        $this -> name = $address1;
        $this -> socket = $socket1;
        $this -> status = 0;
        $this -> lastHB =time();
        mylog("User " . $this -> address . " Login");
    }
    
    /* 成员函数 */
    function receive($data){
        mylog($this -> name . " --> " . $data);
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
            } else {
                $this -> status = -1;
            }
        } else if($this -> status == 2) {
            if($data == "OK") {
                $this -> status = 3;
                $this -> send("OK");
            } else {
                $this -> status = -1;
            }
        }
    }
    
    function check(){
        $time = time() - $this -> lastHB;
        mylog($this -> name . " " . $time . " = " . time() . "-" . $this -> lastHB);
        
        if($time > 15) {
            return false;
        } else {
            return true;
        }
    }
    
    function send($data){
        mylog($this -> name . " <-- " . $data);
        stream_socket_sendto($this -> socket, $data, 0, $this -> address);
    }
}
