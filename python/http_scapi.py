
from flask import Flask, request, render_template, abort
import simplejson

# requires pyOSC
# https://trac.v2.nl/wiki/pyOSC
from OSC import OSCClient,OSCMessage, OSCServer


import sys
import atexit

import string,cgi,time
from os import curdir, sep
from optparse import OptionParser
parser = OptionParser()
parser.add_option("--host",dest="host",help="HTTP host",type=str,default="127.0.0.1")
parser.add_option("--port",dest="port",help="HTTP port",type=int,default="5000")
parser.add_option("--sc_host",dest="sc_host",help="SC OSC host",type=str,default="127.0.0.1")
parser.add_option("--sc_port",dest="sc_port",help="SC OSC port",type=int,default="57120")

(options, args) = parser.parse_args()


app = Flask(__name__)
app.debug = False




@app.route('/')
def index():
    return 'SuperCollider http interface'

@app.route('/fiddle')
def fiddle():
    return render_template("fiddle.html")

@app.route('/call/<path:path>',methods=["POST"])
def call(path):

    data = request.form['data']

    args = []
    msg = OSCMessage("/API/http/call")

    if data:
        try:
            data = simplejson.loads(data)
        except Exception,e:
            # raise bad request
            return ("Bad request", 400, [])
        else:
            for a in data.get('msg',[]):
                args.append(a)
                msg.append(a)

    #client.send(msg)

    # just saying I sent it
    return "%s %s" % (path,args)




if __name__ == "__main__":

    # return_port
    sc = OSCClient()
    sc.connect( (options.sc_host, options.sc_port) )

    # atexit runs whenever python shuts down
    # so it always disconnects no matter what happens
    def off():
        print "\nclosing OSC"
        sc.close()
    atexit.register(off)

    print 'SuperCollider HTTP API'
    app.config['SERVER_NAME'] = "%s:%s" % (options.host,options.port)
    app.run()


