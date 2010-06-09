 BellGen {
	*new {
		SynthDef("bell",{
		|	out, triggerFreq = 0.01, freqScale = 1, freqOffset = 0, decayScale = 1.5, pan = 0, baseSustain = 3, amp = 1
		|
			var sound, audio, trigger, env;

			env = EnvGen.kr(Env([1,1,0],[0.9,0.1],[0,0,0]),timeScale: baseSustain * decayScale,doneAction: 2);

			trigger = Impulse.ar(triggerFreq);
			sound = (
				Klank.ar(`[
					[350, 575, 1250,1375,1600,1850,2000,2875],
					nil,
					[1.9, 1.0, 1.9, 0.2, 1.4, 1.0, 0.75,0.75 ]
				],trigger,freqScale,freqOffset,decayScale) * 0.1
			).softclip;

			audio = Pan2.ar(Mix(env*sound),pan) * amp * 0.5;
			Out.ar(out,audio);
		}).store;
		^super.new;
	}

	chooseActive { |active,subdivisions,depth|
		var left = (0..subdivisions-1);
		var chosen = active.collect { |i|
			var x = left.choose;
			left.remove(x);
			x;
		};
		^subdivisions.collect { |i|
			(chosen.includes(i)).if({depth+1},{-1 * (depth+1)});
		}
	}

	subGen { |intensity,subdivisions,depth|
		var active = max(1,subdivisions * intensity).round.asInteger;
		var chosen = this.chooseActive(active,subdivisions,depth);
		var done;
		(depth < 2).if({
			done = chosen.collect {|n|
				(n >= 1).if({
					((intensity / 1.2) > 1.0.rand).if({this.subGen(intensity,2,depth+1)},{depth+1});
				},{-1 * (depth+1)});
			};
		},{done = chosen});
		^done;
	}

	genPattern { |intensities,base=2,voice=0|
		var x = ("INTENSITIES: " ++ intensities).postln;
		var baseFreqS = [0.5,1,2].choose;
		var freqChoices = [
			[baseFreqS],
			[baseFreqS,1.2*baseFreqS,1.5*baseFreqS]
		].choose.postln;
		var intensity = intensities[0];
		var subdivisions = [2,3,4].choose;
		var rhythm = this.subGen(min(intensity+0.2,1),subdivisions,0).postln;
		var baseLength = (base / subdivisions);
		var durs = rhythm.flat.collect {|n|
			base / (subdivisions * (2 ** (n.abs-1)));
		};
		var freqs = rhythm.flat.collect {|n|
			(n < 0).if({\r},{1});
		};
		var amps = intensities.collect {|i| durs.collect {|d| i }}.flat;
		var sustains = durs.collect {|d,i| 3 * d };

		^Pbind(*[
			instrument: \bell,
			freqScale: Pfunc({ freqChoices.choose }),
			dur: Pseq(durs,inf),
			freq: Pseq(freqs,inf),
			amp: Pseq(amps),
			decayScale: Pseq(sustains,inf),
			voice: voice,
		]);
	}
}                                               