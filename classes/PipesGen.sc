 PipesGen {
	/* generates a pipe synth.
		  index:     any integer value. will be appended the synth's name. use to avoid collisions
		  count:     the number of Klanks to mix together
		  n:         the number of pitches per Klank
		  intervals: the scale degrees of the base frequency of each successive Klank
	*/
  pipesGen { |index=0,count=1,n=5,intervals=(#[0,3,5,7,10,12,15,17,19]),baseFreq = 60|
    ^SynthDef("pipes"++count++"c"++n++"n"++index,{
    | out, sustain = 10, fadeIn = 0.05, fadeOut = 0.05,
      amp = 1.0, variance = 0.05
    |
			var audio,env;

			env = EnvGen.kr(Env([0,1,1,0],[fadeIn,1-fadeIn-fadeOut,fadeOut],[-2,0,-2]),timeScale: sustain,doneAction: 2);

			audio = Mix.arFill(count, { |i|
				var noise,specs,prePanned,freq;
				
				noise = PinkNoise.ar;
				freq = (intervals.at(i) + baseFreq).midicps;
				specs = `[
					Array.fill(n, {|j| (j+1) * freq}),	// frequencies
					nil,																		// amps default to 1.0
					Array.fill(n, {10.0.rand})					// ring times
				];
				prePanned = Klank.ar(specs,noise);
				Pan2.ar(prePanned, i * 0.2 - 0.5);
			});

			6.do({ audio = AllpassN.ar(audio, 0.1, [variance.rand,variance.rand], 4) });
			audio = audio * env * 0.006 * amp;
			Out.ar(out,audio);
		});
	}

	/* generates a random interval list for use with pipesGen */
	randIntervals { |length = 20|
		var max = 3.rand + 2;
		^Array.rand(length,max(1,max-3),max);
	}

	/* generates a pipe synth based on an intensity (and optionally a set of intervals) */
	genPipeSynth { |intensity = nil, intervals = nil|
		var count,n;
		(intensity == nil).if({ intensity = 1.0.rand; intensity.postln;});
		(intervals == nil).if({ intervals = this.randIntervals; });
		count = max(1,((2.0.rand+1) * intensity * 6).round);
		count.postln;
		n = max(1,((2.0.rand+1) * intensity * 5).round);
		n.postln;
		^this.pipesGen(0,count,n,intervals);
	}

	/* generates and stores a pipe synth based on an intensity */
	genStorePipeSynth { |intensity = nil, intervals = nil|
		var synth = this.genPipeSynth(intensity,intervals);
		^synth.store;
	}

	/* returns a pattern that uses a generated pipe synth */
	genPattern { |intensities,length,voice|
		var synth = this.genStorePipeSynth(intensities[0]);

		^Pbind(*[
			sustain: 2.1 * length,
			dur:			2 * Pseq([length]),
			instrument: synth.name,
			fadeIn: 0.2,
			fadeOut: 0.2,
			/* these are here for debugging purposes only */
			voice: voice,
			intensity: intensities[0],
		]);
	}
}  