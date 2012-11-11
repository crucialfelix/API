

(
	interpret: { arg reply, code;
		var result;
		result = thisProcess.interpreter.interpret(code.asString);
		// lists and dicts supported later
		if(result.isNumber.not,{
			result = result.asString
		});
		reply.value( result.asString );
	},
	executeFile: { arg reply, path;
		var result = thisProcess.interpreter.executeFile(path.asString);
		reply.value( result.asString );
	}
);

