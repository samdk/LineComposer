SimpleLineComposer {
	var<> line;
	var<> piece;
	var<> bellGen;
	var<> pipesGen;
	
	*new { |line=#[0.1,0.17,0.24,0.31,0.38,0.45,0.52,0.59,0.66,0.73,0.8,0.87,0.94,1,0.8,0.6,0.4,0.2,0.1,0.17,0.24,0.31,0.38,0.45,0.52,0.59,0.66,0.7,0.7,0.7,0.7,0.7,0.7,0.7,0.7]|
		var me = super.new;
		me.line = line;
		me.bellGen = BellGen.new;
		me.pipesGen = PipesGen.new;
		^me;
	}

	genPiece {
		this.piece = this.voiceGen(this.line);
		^this.piece;
	}
	
	playPiece {
		(this.piece == nil).if({ this.genPiece; });
		this.piece.play;
		^this.piece;
	}

	genVoiceCounts { |intensities|
		^intensities.collect { |i| (5 * i).round.max(1) };
	}

	separateVoices { |intensities|
		var voiceCounts = this.genVoiceCounts(intensities);

		^(5.collect { |c| voiceCounts.collect { |v,i| (v >= c).if({intensities[i]},{0}) } });
	}

	restUntil { |first,voice = 0|
		^Pbind(*[ freq: Pseq([\r],first), dur: 2, voice: voice ]);
	}

	voiceFor { |intensities, length, voice = 0|
		^([true,false].choose).if({
			this.bellGen.genPattern(intensities,voice);
		},{
			this.pipesGen.genPattern(intensities,length,voice);
		});
	}

	segmentVoice { |voice,intensities,voiceNumber|
		var segments = Array.new(voice.size + 1);
		var countSegments = Array.new(voice.size + 1);
		var isZero = true;
		var vals = Array.new(intensities.size);
		var count = 0;
		("size: " ++ intensities.size).postln;

		("start"++voiceNumber).postln;

		voice.do { |v,i|
			(v == 0 && isZero).if({count = count + 1;});
			(v != 0 && isZero.not).if({ count = count + 1; vals.add(v)});

			(v != 0 && isZero).if({
				segments.add(this.restUntil(count,voiceNumber));
				isZero = false;
				vals.add(v);
				countSegments.add(count);
				count = 1;
			});
			(v == 0 && isZero.not).if({
				segments.add(this.voiceFor(vals,count,voiceNumber));
				isZero = true;
				vals = Array.new(intensities.size);
				countSegments.add(count);
				count = 1;
			});
		};
		(isZero).if({
			segments.add(this.restUntil(count,voiceNumber));
			isZero = false;
			countSegments.add(count);
			count = 1;
		});
		(isZero.not).if({
			(vals.size > 0).if({segments.add(this.voiceFor(vals,count,voiceNumber));});
			isZero = true;
			countSegments.add(count);
			count = 1;
		});
		^segments;
	}

	voiceGen { |intensities|
		var voices = this.separateVoices(intensities);

		^Ppar(voices.collect { |voice,i|
			var segments = this.segmentVoice(voice,intensities,i);
			Pseq(segments);
		});
	}
}
                                                            