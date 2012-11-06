
from flask import Flask, request, render_template, abort
import simplejson

# requires pyOSC
# https://trac.v2.nl/wiki/pyOSC
from OSC import OSCClient,OSCMessage, OSCServer


import sys
import atexit

import string,cgi,time
from os import curdir, sep

# defaults
http_port = 5000
http_url = "127.0.0.1"

osc_host = "127.0.0.1"
osc_port = 12000

debug = True


app = Flask(__name__)

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



if __name__ == '__main__':

    # will get better args
    if sys.argv[1:]:
        http_port = int(sys.argv[1])

    # debug
    app.debug = debug

    try:
        sc = OSCServer( (osc_host,osc_port) )
        #atexit.register(sc.close)
        client = OSCClient(sc)
        print 'starting HTTP to SuperCollider API bridge...'
        app.run()
    except KeyboardInterrupt:
        print '^C received, shutting down server'
        #server.socket.close()
        sc.close()


