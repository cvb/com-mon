# prereq
better to use something like `export PATH=./.bin:$PATH` in you shell init
so you don't need to print full path all the time
next env variables should be set:
- AWS_ACCESS_KEY
- AWS_SECRET_ACCESS_KEY
- AWS_REGION

one of the ways to do that is to put them somewhere like ~/.profile
```
export AWS_ACCESS_KEY=access-key
export AWS_SECRET_ACCESS_KEY=secret-key
export AWS_REGION=eu-west-1
```

# install
`docker-compose up` should start repl

# update
`.bin/update-l` will do update

# tools
- `exc` will allow to exec arguments in context of neo4j container
  neo4j use some kind of complex protocol with variable ports so they
  are not exposed and you can't just `docker run` command
- neo4j-shell convenience shell wrapper for conteinerized docker
