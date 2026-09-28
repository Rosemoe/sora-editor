---@class Person
---@field name string
---@field age integer
local Person = {}
Person.__index = Person

---@param name string
---@param age integer
---@return Person
function Person.new(name, age)
    return setmetatable({ name = name, age = age }, Person)
end

---@param years integer
function Person:grow(years)
    self.age = self.age + years
end

---@param person Person
---@return string
local function describe(person)
    return string.format("%s is %d years old", person.name, person.age)
end

local person = Person.new("Sora", 6)
person:grow(1)
print(describe(person))
