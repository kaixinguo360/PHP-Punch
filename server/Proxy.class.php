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
            } else {
                $this -> status = -1;
            }
        } else if($this -> status == 2) {
            if($data == "OK") {
                $this -> status = 3;
                $this -> send("OK");
                $this -> mylog($this -> name . " >>> Login!");
            } else {
                $this -> status = -1;
            }
        } else if($this -> status == 3) {
            if($data != "HB") {
                $this -> toDo($data);
            }
        }
    }
    
    function toDo($data){
        
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
        if($p < 1) {
            mylog($data);
        }
    }
}
