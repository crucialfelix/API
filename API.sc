

API {
	
	classvar <all,<listeners,<>defaultResponse ='/response';
	var <name,functions;
	var oscResponders;
	
	*new { arg name;
		^(all.at(name.asSymbol) ?? {super.new.init(name.asSymbol)});
	}
	init { arg n;
		name = n;
		functions = Dictionary.new;
		all.put(name,this);
	}
	*initClass {
		all = IdentityDictionary.new;
		listeners = Dictionary.new;
	}

	// defining
	add { arg selector,func;
		functions.put(selector,func)
	}
	addAll { arg dict;
		functions.putAll(dict)
	}
	make { arg func;
		this.addAll(Environment.make(func))
	}
	exposeMethods { arg obj, selectors;
		selectors.do({ arg m;
			this.add(m,{ arg ... args; obj.performList(m,args) })
		})
	}
	exposeAllExcept { arg obj,selectors=#[];
		obj.class.methods.do({ arg meth;
			if(selectors.includes(meth.name).not,{
				this.add(meth.name,{ arg ... args; obj.performList(meth.name,args) })
			})
		})
	}	
	
	// calling
	call { arg selector ... args;
		var m;
		m = functions[selector] ?? {Error(selector.asString + "not found in API" + name).throw};
		^m.valueArray(args)
	}
	// create a function
	func { arg selector ... args;
		^{ arg ... ags; this.call(selector,*(args ++ ags)) }
	}
	// respond as though declared functions were native methods to this object
	doesNotUnderstand { arg selector ... args;
		^this.call(selector,*args)
	}
	// '/app/cmdName', arg1, arg2
	*call { arg selector ... args;
		var blank,app,cmd;
		# blank,app ... cmd = selector.asString.split($/);
		^this(app).call(cmd.join($/).asSymbol,*args)
	}	

	// OSC
	mountOSC { arg baseCmdName,addr;
		// default: this.name, nil addr = from anywhere 
		this.unmountOSC;
		functions.keysValuesDo({ arg k,f;
			var r;
			r = OSCresponderNode(addr,
					("/" ++ (baseCmdName ? name).asString ++ "/" ++ k.asString).asSymbol,
					{ arg time,resp,message,addr;
						var result,returnAddr,returnPath;
						result = API.prFormatResult( this.call(k,*message[1..]) );
						# returnAddr,returnPath = API.prResponsePath(addr); 
						returnAddr.sendMsg(*([returnPath] ++ result));
					}).add;
			oscResponders = oscResponders.add( r );
		});
		oscResponders = oscResponders.addAll( [
			// yes, these overwrite any at this addr / path
			// even if for other APIs, because its the same action
			// and callback paths are /absolute
			// may change this
			OSCresponder(addr,'/API/registerListener',
				{ arg time,resp,message,addr;
					var listeningPort,callbackCmdName,blah,hostname;
					if(message.size == 3,{
						# blah,listeningPort,callbackCmdName = message;
					},{
						# blah, listeningPort = message;
					});
					API.registerListener(addr,NetAddr.fromIP(addr.addr,listeningPort),callbackCmdName);
				}
			).add,
			OSCresponder(addr,'/API/call',
				{ arg time,resp,message,addr;
					var pathToSendReturnValue, apiCallPath,args,blah,returnAddr,returnPath,result;
					# blah, pathToSendReturnValue, apiCallPath ... args = message;
					result = API.prFormatResult( API.call(apiCallPath,*args) );
					// should support /API/call [returnPath returnIdentifiers] etc.
					# returnAddr,returnPath = API.prResponsePath(addr);
					returnAddr.sendMsg( * ([pathToSendReturnValue] ++ result) )
					
				}
			).add
		]);
		^oscResponders
	}
	unmountOSC {
		oscResponders.do(_.remove);
		oscResponders = nil;
	}		
	// maybe better separated by API
	*registerListener { arg callsFromNetAddr,sendResponseToNetAddr,responseCmdName;
		listeners[callsFromNetAddr] = [sendResponseToNetAddr,responseCmdName];
	}
	
	// interrogating
	functionNames {
		^functions.keys
	}
			
	*prResponsePath { arg addr;
		var l;
		l =listeners[addr];
		if(l.notNil,{
			^[l[0],l[1] ? defaultResponse]
		});
		^[addr,defaultResponse]
	}
	*prFormatResult { arg result;
		^if(result.isString,{
			result = [result];
		},{
			result = result.asArray;
		});
	}	
		
	printOn { arg stream;
		stream << this.class.asString << "('" << name << "')"
	}
	

}


	
	