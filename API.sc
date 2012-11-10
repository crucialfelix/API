

API {

	classvar <all;
	var <name, functions;
	var oscResponders;

	*new { arg name;
		// get or create
		^(all.at(name.asSymbol) ?? { super.new.init(name.asSymbol) })
	}
	*load { arg name;
		// for now, will load it from /apis/
		^all.at(name.asSymbol) ?? { Error("API" + name + "not found").throw; }
	}
	init { arg n;
		name = n;
		functions = Dictionary.new;
		all.put(name, this);
	}
	*initClass {
		all = IdentityDictionary.new;
	}

	// defining
	add { arg selector, func;
		functions.put(selector, func)
	}
	addAll { arg dict;
		functions.putAll(dict)
	}
	// add methods off an object as functions
	exposeMethods { arg obj, selectors;
		selectors.do({ arg m;
			this.add(m, { arg callback ... args;
				callback.value(obj.performList(m, args));
			})
		})
	}
	exposeAllExcept { arg obj, selectors=#[];
		obj.class.methods.do({ arg meth;
			if(selectors.includes(meth.name).not, {
				this.add(meth.name, { arg callback ... args;
					callback.value( obj.performList(meth.name,args) )
				})
			})
		})
	}

	// calling
	async { arg selector, args, callback, onError;
		var m;
		m = this.prFindHandler(selector);
		if(onError.notNil,{
			{
				m.valueArray([callback] ++ args);
			}.try(onError);
		},{
			m.valueArray([callback] ++ args);
		});
	}
	sync { arg selector, args, onError;
		// must be inside a Routine
		var result, c = Condition.new;
		c.test = false;
		this.async(selector, args, { arg r;
			result = r;
			c.test = true;
			c.signal;
		}, onError);
		c.wait;
		^result
	}
	*async { arg apiname, path, args, callback, onError;
		this.load(apiname).async(path, args, callback, onError);
	}
	*sync { arg apiname, path, args, onError;
		^this.load(apiname).sync(path, args, onError);
	}


	// calls and returns immediately
	// no async, no Routine required
	// '/apiname/cmdName', arg1, arg2
	*call { arg selector ... args;
		var blank, app, cmd;
		# blank, app ... cmd = selector.asString.split($/);
		^this.load(app).call(cmd.join($/).asSymbol,*args);
	}
	call { arg selector ... args;
		var m = this.prFindHandler(selector), result;
		m.valueArray([{ arg r; result = r; }] ++ args);
		^result
	}

	// for ease of scripting
	// respond as though declared functions were native methods to this object
	doesNotUnderstand { arg selector ... args;
		if(thisThread.class === Thread, {
			^this.call(selector,*args)
		},{
			^this.sync(selector,args)
		})
	}

	prFindHandler { arg path;
		^functions[path] ?? {
			Error(path.asString + "not found in API" + name).throw
		}
	}


	mountOSC { arg baseCmdName, addr;
		// simply registers each function in this API as an OSC responder node
		// baseCmdName : defaults to this.name  ie.  /{this.name}/{path}
		// addr:  default is nil meaning accept message from anywhere
		this.unmountOSC;
		functions.keysValuesDo({ arg k, f;
			var r;
			r = OSCresponderNode(addr,
					("/" ++ (baseCmdName ? name).asString ++ "/" ++ k.asString).asSymbol,
					{ arg time, resp, message, addr;
						this.call(k,*message[1..]);
					}).add;
			oscResponders = oscResponders.add( r );
		});
		^oscResponders
	}
	unmountOSC {
		oscResponders.do(_.remove);
		oscResponders = nil;
	}

	// duplex returns results of API calls as a reply OSC message
	*mountDuplexOSC { arg srcID, recvPort;
		/*
			/API/call : client_id, request_id, fullpath ... args

			/API/reply : client_id, request_id, result
			/API/not_found : client_id, request_id, fullpath
			/API/error : client_id, request_id, errorString
		*/
		OSCdef('API_DUPLEX', { arg msg, time, addr, recvPort;
			var client_id, request_id, path, args, api, apiName, fullpath, m, ignore;
			# ignore, client_id, request_id, fullpath ... args = msg;
			# ignore, apiName ... path = fullpath.asString.split($/);
			path = path.first.asSymbol;

			// [msg,time,addr,recvPort].debug;
			// [client_id, request_id, fullpath, blank, apiName, path].debug;

			{
				api = this.load(apiName);
				m = api.prFindHandler(path);
			}.try({ arg error;
				addr.sendMsg('/API/not_found', client_id, request_id, fullpath);
				error.reportError();
			});
			if(m.notNil,{
				api.async(path, args, { arg result;
					addr.sendMsg('/API/reply', client_id, request_id, result);
				}, { arg error;
					addr.sendMsg('/API/error', client_id, request_id, error.errorString() );
					error.reportError();
				});
			});
		}, '/API/call', srcID, recvPort);
	}
	*unmountDuplexOSC {
		OSCdef('API_DUPLEX').free;
	}

	printOn { arg stream;
		stream << this.class.asString << "('" << name << "')"
	}
}

