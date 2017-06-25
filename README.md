# Relayer

An API that suggests potential stopover cities between a given pair of cities very fast, based only on their location and population. Try it out (and read Swagger documentation) [here](relay-er.herokuapp.com). My version supports only Europe, but this is an artificial constraint to speed it up. It can be removed in `config.clj`

## Usage

There are 2 endpoints:

`https://relay-er.herokuapp.com/city/{id}` gives a complete summary of a city identified by `{id}`

`https://relay-er.herokuapp.com/between/{id1}/{id2}` gives a list of cities between `{id1}` and `{id2}`. If neither`?skip=` nor `?limit=` is defined defaults to 20 best (largest and inbetween-est) cities.

`{id}` can be one of 3 things:
- Relayer ID, i.e. `https://relay-er.herokuapp.com/city/488$23` for Paris. This ID is based on the city's decimal latitude and longitude, multiplied by 10 and rounded down.
- [Geonames](http://www.geonames.org) ID, i.e. `https://relay-er.herokuapp.com/city/2988507`.
- City name, i.e. `https://relay-er.herokuapp.com/city/Paris`. This might seem like the simplest option, but don't send me angry emails if you get the wrong [St.Petersburg](https://en.wikipedia.org/wiki/St._Petersburg,_Florida)

## License

Copyright © 2017 Vadim Liventsev

Distributed under the MIT License, see `LICENSE`.
