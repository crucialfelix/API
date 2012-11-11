

(
	interpret: { arg reply, code;
		var result;
		result = thisProcess.interpreter.interpret(code.asString);
		reply.value( JSON.stringify(('result':result)) );
	},
	executeFile: { arg reply, path;
		var result = thisProcess.interpreter.executeFile(path.asString);
		reply.value( JSON.stringify(('result':result)) );
	}
);

